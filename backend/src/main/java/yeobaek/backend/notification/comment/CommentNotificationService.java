package yeobaek.backend.notification.comment;

import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import com.google.firebase.messaging.FirebaseMessagingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.domain.ClubMembers;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.comment.event.ClubSentenceCommentCreatedEvent;
import yeobaek.backend.comment.event.PublicRoomSentenceCommentCreatedEvent;
import yeobaek.backend.member.domain.Members;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.FcmRetryService;
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

    private final FcmPushSender pushSender;
    private final FcmFailureHandler failureHandler;
    private final FcmRetryService retryService;
    private final NotificationRegistrationService registrationService;
    private final CommentNotificationRecipientQueryRepository recipientQueryRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final SentenceRepository sentenceRepository;
    private final MemberRepository memberRepository;
    private final PlatformTransactionManager transactionManager;

    public void notifyClub(ClubSentenceCommentCreatedEvent event) {
        Map<String, String> data = Map.of(
                "type", "CLUB_COMMENT", "clubId", event.clubId().toString(),
                "passageSequence", Integer.toString(event.passageSequence()), "sentenceId", event.sentenceId().toString());
        notifyRecipients(event.commentId(), event.commentCreatedAt(),
                () -> findReadersInClub(event.clubId(), event.sentenceId(), event.writerId()), data);
    }

    public void notifyPublicRoom(PublicRoomSentenceCommentCreatedEvent event) {
        Map<String, String> data = Map.of(
                "type", "PUBLIC_ROOM_COMMENT", "publicRoomId", event.publicRoomId().toString(),
                "passageSequence", Integer.toString(event.passageSequence()), "sentenceId", event.sentenceId().toString());
        notifyRecipients(event.commentId(), event.commentCreatedAt(),
                () -> findPreviousCommentersInPublicRoom(
                        event.publicRoomId(), event.sentenceId(), event.writerId()), data);
    }

    private void notifyRecipients(Long commentId, Instant commentCreatedAt,
                                  Supplier<NotificationRegistrations> recipients, Map<String, String> data) {
        try {
            // 조회에만 트랜잭션을 적용하고 FCM 호출 전에 종료한다.
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setReadOnly(true);
            NotificationRegistrations sentRegistrations = transaction.execute(status -> recipients.get());
            List<FcmRetryTask> tasks = sendAndCreateRetryTasks(commentId, commentCreatedAt, sentRegistrations, data);
            tasks.forEach(retryService::schedule);
            log.atInfo().addKeyValue(OPERATION, "notification.comment.logRetryTasks")
                    .addKeyValue(RESULT, "success").addKeyValue(COMMENT_ID, commentId)
                    .addKeyValue("retryTaskCount", tasks.size())
                    .log("댓글 알림의 재시도 후보 작업 수를 확인했습니다.");
        } catch (FirebaseMessagingException exception) {
            failureHandler.handleCallFailure(commentId, exception);
        } catch (IllegalArgumentException exception) {
            failureHandler.handleInvalidRequest(commentId);
        } catch (DataAccessException exception) {
            log.atError().addKeyValue(OPERATION, "notification.comment.logFailure")
                    .addKeyValue(RESULT, "failure").addKeyValue(COMMENT_ID, commentId)
                    .setCause(exception).log("댓글 저장 후 알림 처리에 실패했습니다.");
        }
    }

    private NotificationRegistrations findReadersInClub(Long clubId, Long sentenceId, Long writerId) {
        ClubMembers targets = new ClubMembers(
                clubMemberRepository.findNotificationCandidatesWithReadingProgress(clubId, writerId));
        Members recipients = new Members(targets.otherMembersAheadInReadingProgress(
                sentenceRepository.getReferenceById(sentenceId), memberRepository.getReferenceById(writerId)));
        return registrationService.findByMembers(recipients);
    }

    private NotificationRegistrations findPreviousCommentersInPublicRoom(
            Long publicRoomId, Long sentenceId, Long writerId) {
        return new NotificationRegistrations(
                recipientQueryRepository.findRegistrationsForPublicRoomComment(publicRoomId, sentenceId, writerId));
    }

    private List<FcmRetryTask> sendAndCreateRetryTasks(
            Long commentId, Instant commentCreatedAt, NotificationRegistrations sentRegistrations, Map<String, String> data)
            throws FirebaseMessagingException {
        List<String> firebaseInstallationIds = sentRegistrations.firebaseInstallationIds();
        if (firebaseInstallationIds.isEmpty()) {
            log.debug("알림 수신 대상이 존재하지 않아 알림을 보내지 않습니다.");
            return List.of();
        }

        FcmSendResult result;
        try {
            result = pushSender.send(firebaseInstallationIds, TITLE, BODY, data);
        } catch (FirebaseMessagingException exception) {
            Instant observedAt = Instant.now();
            FcmSendFailure failure = FcmSendFailure.from(null, exception);
            if (!failure.isRetryable()) {
                throw exception;
            }
            failureHandler.handleCallFailure(commentId, exception);
            return createCallRetryTasks(commentId, commentCreatedAt, observedAt, failure, sentRegistrations, data);
        }
        // SDK 개별 HTTP 수신 시각이 아니라, 호출이 끝나 결과를 확인한 애플리케이션 시각이다.
        Instant observedAt = Instant.now();
        failureHandler.handle(result, sentRegistrations);
        return createRetryTasks(commentId, commentCreatedAt, observedAt, result, sentRegistrations, data);
    }

    private List<FcmRetryTask> createRetryTasks(
            Long commentId, Instant commentCreatedAt, Instant observedAt,
            FcmSendResult result, NotificationRegistrations sentRegistrations, Map<String, String> data) {
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
            tasks.add(new FcmRetryTask(commentId, registrationIds, TITLE, BODY, data, failure,
                    commentCreatedAt, observedAt, 0, false));
        }
        return List.copyOf(tasks);
    }

    private List<FcmRetryTask> createCallRetryTasks(Long commentId, Instant commentCreatedAt, Instant observedAt,
                                                  FcmSendFailure failure, NotificationRegistrations sentRegistrations,
                                                  Map<String, String> data) {
        // 개별 결과는 알 수 없으므로 failure.fid는 null로 유지하고 원래 등록에만 추가 시도 대상을 연결한다.
        List<FcmRetryTask> tasks = new ArrayList<>();
        for (String fid : sentRegistrations.firebaseInstallationIds()) {
            tasks.add(new FcmRetryTask(commentId, sentRegistrations.idsFor(new FirebaseInstallationId(fid)),
                    TITLE, BODY, data, failure, commentCreatedAt, observedAt, 0, false, fid));
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
