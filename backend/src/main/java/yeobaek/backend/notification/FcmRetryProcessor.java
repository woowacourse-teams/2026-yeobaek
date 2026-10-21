package yeobaek.backend.notification;

import static yeobaek.backend.notification.domain.FcmSendFailure.MAX_JITTER_PERCENT;
import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import com.google.firebase.messaging.FirebaseMessagingException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.TaskScheduler;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

@RequiredArgsConstructor
@Slf4j
public class FcmRetryProcessor {

    private final TaskScheduler scheduler;
    private final TaskExecutor executor;
    private final Clock clock;
    private final FcmPushSender sender;
    private final NotificationRegistrationService registrationService;
    private final FcmFailureHandler failureHandler;
    private final Semaphore pendingSlots;

    public void schedule(FcmRetryTask task) {
        if (task.isExpired(clock.instant())) {
            logStopped(task, "EXPIRED");
            return;
        }
        int jitterPercent = ThreadLocalRandom.current().nextInt(MAX_JITTER_PERCENT + 1);
        var nextRetryAt = task.nextRetryAt(jitterPercent);
        if (nextRetryAt.isEmpty()) {
            logStopped(task, task.stopReason(jitterPercent).orElseThrow());
            return;
        }
        if (!pendingSlots.tryAcquire()) {
            logRejected(task, "pendingCapacityExceeded");
            return;
        }
        try {
            scheduler.schedule(() -> {
                // 예약 대기만 제한한다. 실행기 대기열과 실행 중 작업은 기존 실행기의 상한을 따른다.
                pendingSlots.release();
                submit(task);
            }, nextRetryAt.orElseThrow());
            log.atInfo().addKeyValue(OPERATION, "notification.retry.schedule").addKeyValue(RESULT, "success")
                    .addKeyValue(COMMENT_ID, task.commentId()).addKeyValue("retriesPerformed", task.retriesPerformed())
                    .addKeyValue("scope", task.isCallFailure() ? "call" : "target")
                    .addKeyValue("nextRetryAt", nextRetryAt.orElseThrow()).log("FCM 추가 발송을 예약했습니다.");
        } catch (TaskRejectedException exception) {
            pendingSlots.release();
            logRejected(task, "schedulerRejected");
        }
    }

    private void submit(FcmRetryTask task) {
        try {
            CompletableFuture.runAsync(() -> execute(task), executor).whenComplete((ignored, exception) -> {
                if (exception != null) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    log.atError().addKeyValue(OPERATION, "notification.retry.submit").addKeyValue(RESULT, "failure")
                            .addKeyValue(COMMENT_ID, task.commentId()).addKeyValue("reason", "unexpectedFailure")
                            .addKeyValue("exceptionType", cause.getClass().getSimpleName())
                            .log("재시도 작업의 예상하지 못한 오류로 해당 작업을 종료했습니다.");
                }
            });
        } catch (TaskRejectedException exception) {
            logRejected(task, "executorRejected");
        }
    }

    private void execute(FcmRetryTask task) {
        try {
            if (task.isExpired(clock.instant())) {
                logStopped(task, "EXPIRED");
                return;
            }
            var fid = new FirebaseInstallationId(task.fid());
            NotificationRegistrations snapshot = registrationService.snapshotForRetry(task.registrationIds(), fid);
            List<Long> remainingIds = snapshot.idsFor(fid);
            if (remainingIds.isEmpty()) {
                logStopped(task, "REGISTRATION_REMOVED_OR_CHANGED");
                return;
            }
            if (task.isExpired(clock.instant())) {
                logStopped(task, "EXPIRED");
                return;
            }
            send(task, snapshot, remainingIds);
        } catch (IllegalArgumentException exception) {
            failureHandler.handleInvalidRequest(task.commentId());
        } catch (DataAccessException exception) {
            log.atError().addKeyValue(OPERATION, "notification.retry.execute").addKeyValue(RESULT, "failure")
                    .addKeyValue(COMMENT_ID, task.commentId()).addKeyValue("exceptionType", exception.getClass().getSimpleName())
                    .log("재시도 대상의 등록 정보를 확인하지 못해 해당 작업을 종료했습니다.");
        }
    }

    private void send(FcmRetryTask task, NotificationRegistrations snapshot, List<Long> remainingIds) {
        try {
            FcmSendResult result = sender.send(List.of(task.fid()), task.title(), task.body(), task.data());
            Instant observedAt = clock.instant();
            failureHandler.handle(result, snapshot);
            if (result.failureCount() == 0) {
                log.atInfo().addKeyValue(OPERATION, "notification.retry.send").addKeyValue(RESULT, "success")
                        .addKeyValue(COMMENT_ID, task.commentId())
                        .addKeyValue("retriesPerformed", task.retriesPerformed() + 1)
                        .log("추가 발송을 FCM에서 접수해 재시도를 종료했습니다.");
                return;
            }
            scheduleNext(task, result.failures().getFirst(), observedAt, remainingIds);
        } catch (FirebaseMessagingException exception) {
            failureHandler.handleCallFailure(task.commentId(), exception);
            // 원래 실패 대상 하나만 보낸 호출의 예외다. 응답이 없으면 접수 불명확 정책으로 분류한다.
            scheduleNext(task, FcmSendFailure.from(null, exception), clock.instant(), remainingIds);
        }
    }

    private void scheduleNext(FcmRetryTask task, FcmSendFailure failure, Instant observedAt, List<Long> remainingIds) {
        var next = task.afterFailure(failure, observedAt, remainingIds);
        if (next.isPresent()) {
            schedule(next.orElseThrow());
        } else {
            logStopped(task, "NOT_RETRYABLE", task.retriesPerformed() + 1,
                    task.uncertainRetryUsed() || task.failure().isReceiptUncertain());
        }
    }

    private void logStopped(FcmRetryTask task, String reason) {
        logStopped(task, reason, task.retriesPerformed(), task.uncertainRetryUsed());
    }

    private void logStopped(FcmRetryTask task, String reason, int retriesPerformed, boolean uncertainRetryUsed) {
        log.atInfo().addKeyValue(OPERATION, "notification.retry.logStopped").addKeyValue(RESULT, "rejected")
                .addKeyValue(COMMENT_ID, task.commentId()).addKeyValue("stopReason", reason)
                .addKeyValue("scope", task.isCallFailure() ? "call" : "target")
                .addKeyValue("retriesPerformed", retriesPerformed)
                .addKeyValue("uncertainRetryUsed", uncertainRetryUsed).log("FCM 재시도를 종료했습니다.");
    }

    private void logRejected(FcmRetryTask task, String reason) {
        log.atWarn().addKeyValue(OPERATION, "notification.retry.logRejected").addKeyValue(RESULT, "rejected")
                .addKeyValue(COMMENT_ID, task.commentId()).addKeyValue("reason", reason)
                .log("예약기 또는 실행기의 용량·종료 상태로 재시도 작업을 종료했습니다.");
    }
}
