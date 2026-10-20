package yeobaek.backend.notification.comment;

import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import com.google.firebase.messaging.FirebaseMessagingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentNotificationService {
    private static final String TITLE = "새로운 댓글이 달렸어요!";
    private static final String BODY = "댓글을 남겼던 문장의 이야기를 확인해 보세요.";

    private final CommentNotificationRecipientReader recipientReader;
    private final FcmPushSender pushSender;
    private final NotificationRegistrationService registrationService;
    private final FcmFailureHandler failureHandler;

    public List<FcmRetryTask> notifyReadersInClub(
            Long commentId, Long clubId, Long sentenceId, Long writerId, Instant commentCreatedAt)
            throws FirebaseMessagingException {
        List<String> firebaseInstallationIds = recipientReader.findReadersInClub(clubId, sentenceId, writerId);

        return notifyRecipients(commentId, commentCreatedAt, firebaseInstallationIds);
    }

    public List<FcmRetryTask> notifyPreviousCommentersInPublicRoom(
            Long commentId, Long publicRoomId, Long sentenceId, Long writerId, Instant commentCreatedAt)
            throws FirebaseMessagingException {
        List<String> firebaseInstallationIds = recipientReader.findPreviousCommentersInPublicRoom(
                publicRoomId, sentenceId, writerId);

        return notifyRecipients(commentId, commentCreatedAt, firebaseInstallationIds);
    }

    private List<FcmRetryTask> notifyRecipients(
            Long commentId, Instant commentCreatedAt, List<String> firebaseInstallationIds)
            throws FirebaseMessagingException {
        if (firebaseInstallationIds.isEmpty()) {
            log.debug("알림 수신 대상이 존재하지 않아 알림을 보내지 않습니다.");
            return List.of();
        }

        NotificationRegistrations sentRegistrations = registrationService.snapshotForSend(firebaseInstallationIds);
        FcmSendResult result;
        try {
            result = pushSender.send(firebaseInstallationIds, TITLE, BODY);
        } catch (FirebaseMessagingException exception) {
            Instant observedAt = Instant.now();
            FcmSendFailure failure = FcmSendFailure.from(null, exception);
            if (!failure.isRetryable()) {
                throw exception;
            }
            failureHandler.handleCallFailure(commentId, exception);
            return createCallRetryTasks(commentId, commentCreatedAt, observedAt, failure, sentRegistrations);
        }
        // SDK 개별 HTTP 수신 시각이 아니라, 호출이 끝나 결과를 확인한 애플리케이션 시각이다.
        Instant observedAt = Instant.now();
        failureHandler.handle(result, sentRegistrations);
        return createRetryTasks(commentId, commentCreatedAt, observedAt, result, sentRegistrations);
    }

    private List<FcmRetryTask> createRetryTasks(
            Long commentId, Instant commentCreatedAt, Instant observedAt,
            FcmSendResult result, NotificationRegistrations sentRegistrations) {
        List<FcmRetryTask> tasks = new ArrayList<>();
        for (FcmSendFailure failure : result.retryableFailures()) {
            List<Long> registrationIds = sentRegistrations.idsFor(new FirebaseInstallationId(failure.fid()));
            if (registrationIds.isEmpty()) {
                log.atWarn().addKeyValue(OPERATION, "notification.comment.createRetryTasks")
                        .addKeyValue(RESULT, "rejected").addKeyValue(COMMENT_ID, commentId)
                        .addKeyValue("reason", "registrationSnapshotMissing")
                        .log("발송 당시 등록이 없어 재시도 후보 작업을 만들지 않았습니다.");
                continue;
            }
            tasks.add(new FcmRetryTask(commentId, registrationIds, TITLE, BODY, failure,
                    commentCreatedAt, observedAt, 0, false));
        }
        return List.copyOf(tasks);
    }

    private List<FcmRetryTask> createCallRetryTasks(Long commentId, Instant commentCreatedAt, Instant observedAt,
                                                  FcmSendFailure failure, NotificationRegistrations sentRegistrations) {
        // 개별 결과는 알 수 없으므로 failure.fid는 null로 유지하고 원래 등록에만 추가 시도 대상을 연결한다.
        List<FcmRetryTask> tasks = new ArrayList<>();
        for (String fid : sentRegistrations.firebaseInstallationIds()) {
            tasks.add(new FcmRetryTask(commentId, sentRegistrations.idsFor(new FirebaseInstallationId(fid)),
                    TITLE, BODY, failure, commentCreatedAt, observedAt, 0, false, fid));
        }
        if (tasks.isEmpty()) {
            log.atWarn().addKeyValue(OPERATION, "notification.comment.createCallRetryTasks")
                    .addKeyValue(RESULT, "rejected").addKeyValue(COMMENT_ID, commentId)
                    .addKeyValue("reason", "registrationSnapshotMissing")
                    .log("호출 전체 실패의 발송 당시 등록이 없어 추가 시도 작업을 만들지 않았습니다.");
        }
        return List.copyOf(tasks);
    }
}
