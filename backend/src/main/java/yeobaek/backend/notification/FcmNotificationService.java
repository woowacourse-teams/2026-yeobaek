package yeobaek.backend.notification;

import static yeobaek.backend.support.LogField.CLUB_ID;
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
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmNotificationService {
    private final FcmPushSender pushSender;
    private final FcmFailureHandler failureHandler;
    private final FcmRetryService retryService;
    private final PlatformTransactionManager transactionManager;

    public void notifyRecipients(NotificationType type, Long sourceId, Instant createdAt,
                                 Supplier<NotificationRegistrations> recipients,
                                 String title, String body, Map<String, String> data) {
        try {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setReadOnly(true);
            NotificationRegistrations snapshot = transaction.execute(status -> recipients.get());
            // 수신자 조회 트랜잭션을 종료한 뒤 FCM을 호출해요.
            List<FcmRetryTask> tasks = sendAndCreateRetryTasks(type, sourceId, createdAt, snapshot, title, body, data);
            tasks.forEach(retryService::schedule);
            sourceLog(log.atInfo(), type, sourceId, "logRetryTasks")
                    .addKeyValue(RESULT, "success").addKeyValue("retryTaskCount", tasks.size())
                    .log("알림의 재시도 후보 작업 수를 확인했습니다.");
        } catch (FirebaseMessagingException exception) {
            failureHandler.handleCallFailure(type, sourceId, exception);
        } catch (IllegalArgumentException exception) {
            failureHandler.handleInvalidRequest(type, sourceId);
        } catch (DataAccessException exception) {
            sourceLog(log.atError(), type, sourceId, "logFailure")
                    .addKeyValue(RESULT, "failure").setCause(exception)
                    .log("업무 커밋 후 알림 처리에 실패했습니다.");
        }
    }

    private List<FcmRetryTask> sendAndCreateRetryTasks(
            NotificationType type, Long sourceId, Instant createdAt, NotificationRegistrations snapshot,
            String title, String body, Map<String, String> data) throws FirebaseMessagingException {
        List<String> fids = snapshot.firebaseInstallationIds();
        if (fids.isEmpty()) {
            return List.of();
        }
        FcmSendResult result;
        try {
            result = pushSender.send(fids, title, body, data);
        } catch (FirebaseMessagingException exception) {
            Instant observedAt = Instant.now();
            FcmSendFailure failure = FcmSendFailure.from(null, exception);
            if (!failure.isRetryable()) {
                throw exception;
            }
            failureHandler.handleCallFailure(type, sourceId, exception);
            return createCallRetryTasks(type, sourceId, createdAt, observedAt, failure, snapshot, title, body, data);
        }
        // SDK의 개별 HTTP 수신 시각이 아니라 호출 결과를 확인한 시각이에요.
        Instant observedAt = Instant.now();
        failureHandler.handle(result, snapshot);
        return createRetryTasks(type, sourceId, createdAt, observedAt, result, snapshot, title, body, data);
    }

    private List<FcmRetryTask> createRetryTasks(
            NotificationType type, Long sourceId, Instant createdAt, Instant observedAt,
            FcmSendResult result, NotificationRegistrations snapshot, String title, String body, Map<String, String> data) {
        List<FcmRetryTask> tasks = new ArrayList<>();
        for (FcmSendFailure failure : result.retryableFailures()) {
            List<Long> registrationIds = snapshot.findRegistrationIdsByFid(new FirebaseInstallationId(failure.fid()));
            if (registrationIds.isEmpty()) {
                sourceLog(log.atWarn(), type, sourceId, "createRetryTasks")
                        .addKeyValue(RESULT, "rejected").addKeyValue("reason", "registrationSnapshotMissing")
                        .log("발송 당시 등록이 없어 재시도 후보 작업을 만들지 않았습니다.");
                continue;
            }
            tasks.add(new FcmRetryTask(type, sourceId, registrationIds, title, body, data, failure,
                    createdAt, observedAt, 0, false));
        }
        return List.copyOf(tasks);
    }

    private List<FcmRetryTask> createCallRetryTasks(
            NotificationType type, Long sourceId, Instant createdAt, Instant observedAt,
            FcmSendFailure failure, NotificationRegistrations snapshot, String title, String body, Map<String, String> data) {
        // 호출 전체 실패는 개별 접수 여부를 확정하지 않고, 원래 등록에만 재시도 대상을 연결해요.
        List<FcmRetryTask> tasks = new ArrayList<>();
        for (String fid : snapshot.firebaseInstallationIds()) {
            tasks.add(new FcmRetryTask(type, sourceId,
                    snapshot.findRegistrationIdsByFid(new FirebaseInstallationId(fid)),
                    title, body, data, failure, createdAt, observedAt, 0, false, fid));
        }
        return List.copyOf(tasks);
    }

    private LoggingEventBuilder sourceLog(LoggingEventBuilder event, NotificationType type,
                                          Long sourceId, String action) {
        String prefix = type == NotificationType.CLUB_JOIN ? "notification.clubJoin." : "notification.comment.";
        return event.addKeyValue(OPERATION, prefix + action).addKeyValue("notificationType", type.name())
                .addKeyValue(type == NotificationType.CLUB_JOIN ? CLUB_ID : COMMENT_ID, sourceId);
    }
}
