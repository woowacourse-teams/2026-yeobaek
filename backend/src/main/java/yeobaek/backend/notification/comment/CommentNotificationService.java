package yeobaek.backend.notification.comment;

import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import com.google.firebase.messaging.FirebaseMessagingException;
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

    public List<FcmRetryTask> notifyReadersInClub(Long commentId, Long clubId, Long sentenceId, Long writerId)
            throws FirebaseMessagingException {
        List<String> firebaseInstallationIds = recipientReader.findReadersInClub(clubId, sentenceId, writerId);

        return notifyRecipients(commentId, firebaseInstallationIds);
    }

    public List<FcmRetryTask> notifyPreviousCommentersInPublicRoom(
            Long commentId, Long publicRoomId, Long sentenceId, Long writerId)
            throws FirebaseMessagingException {
        List<String> firebaseInstallationIds = recipientReader.findPreviousCommentersInPublicRoom(
                publicRoomId, sentenceId, writerId);

        return notifyRecipients(commentId, firebaseInstallationIds);
    }

    private List<FcmRetryTask> notifyRecipients(Long commentId, List<String> firebaseInstallationIds)
            throws FirebaseMessagingException {
        if (firebaseInstallationIds.isEmpty()) {
            log.debug("알림 수신 대상이 존재하지 않아 알림을 보내지 않습니다.");
            return List.of();
        }

        NotificationRegistrations sentRegistrations = registrationService.snapshotForSend(firebaseInstallationIds);
        FcmSendResult result = pushSender.send(firebaseInstallationIds, TITLE, BODY);
        failureHandler.handle(result, sentRegistrations);
        return createRetryTasks(commentId, result, sentRegistrations);
    }

    private List<FcmRetryTask> createRetryTasks(
            Long commentId, FcmSendResult result, NotificationRegistrations sentRegistrations) {
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
            tasks.add(new FcmRetryTask(commentId, registrationIds, TITLE, BODY, failure));
        }
        return List.copyOf(tasks);
    }
}
