package yeobaek.backend.notification;

import yeobaek.backend.notification.domain.NotificationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;
import yeobaek.backend.support.notification.NotificationExecutorConfiguration;
import yeobaek.backend.support.notification.NotificationRetryConfiguration;

class FcmRetryContextTest {

    private static final ContextKey<String> REQUEST = ContextKey.named("retry-test-request");

    @Test
    @DisplayName("예약 대기를 거쳐도 요청 문맥을 전달하고 FCM 호출은 타이머가 아닌 발송 워커에서 실행한다")
    void preserveContextAcrossTimerAndWorker() throws Exception {
        Clock clock = Clock.systemUTC();
        var scheduler = new NotificationRetryConfiguration().notificationRetryScheduler(clock);
        var executor = new NotificationExecutorConfiguration().notificationTaskExecutor(1, 1);
        var registrations = mock(NotificationRegistrationService.class);
        var sender = mock(FcmPushSender.class);
        var handler = mock(FcmFailureHandler.class);
        var observed = new AtomicReference<List<String>>();
        var finished = new CountDownLatch(1);
        var previousMdc = MDC.getCopyOfContextMap();
        String fid = "c" + "a".repeat(21);
        given(registrations.snapshotForRetry(anyList(), any())).willReturn(new NotificationRegistrations(List.of(
                new NotificationRegistration(1L, mock(Member.class), new FirebaseInstallationId(fid)))));
        given(sender.send(anyList(), anyString(), anyString(), any())).willAnswer(invocation -> {
            observed.set(List.of(MDC.get("traceId"), Context.current().get(REQUEST), Thread.currentThread().getName()));
            finished.countDown();
            return new FcmSendResult(1, List.of());
        });
        var retryService = new FcmRetryService(scheduler, executor, clock, sender, registrations, handler, new Semaphore(1));
        Instant now = clock.instant();
        var task = new FcmRetryTask(NotificationType.CLUB_COMMENT, 11L, List.of(1L), "제목", "내용", Map.of(),
                new FcmSendFailure(fid, "INTERNAL", "INTERNAL", 500, null),
                now.minusSeconds(300), now.minusSeconds(120), 0, false);
        scheduler.initialize();
        executor.initialize();
        try {
            MDC.put("traceId", "original-request");
            Context.current().with(REQUEST, "original-context").wrap(() -> retryService.schedule(task)).run();
            MDC.clear();

            assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(observed.get().subList(0, 2)).containsExactly("original-request", "original-context");
            assertThat(observed.get().get(2)).startsWith("notification-").doesNotStartWith("notification-retry-timer-");
        } finally {
            scheduler.shutdown();
            executor.shutdown();
            if (previousMdc == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousMdc);
            }
        }
    }
}
