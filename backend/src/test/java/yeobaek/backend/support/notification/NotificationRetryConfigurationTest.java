package yeobaek.backend.support.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.registration.service.NotificationRecipientQueryService;

class NotificationRetryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationRetryConfiguration.class)
            .withBean("notificationTaskExecutor", TaskExecutor.class, SyncTaskExecutor::new)
            .withBean(FcmPushSender.class, () -> mock(FcmPushSender.class))
            .withBean(FcmFailureHandler.class, () -> mock(FcmFailureHandler.class))
            .withBean(NotificationRecipientQueryService.class, () -> mock(NotificationRecipientQueryService.class));

    @Test
    @DisplayName("예약 용량이 0이면 잘못된 실행 설정으로 시작하지 않는다")
    void rejectInvalidCapacity() {
        contextRunner.withPropertyValues("notification.retry.pending-capacity=0").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    @DisplayName("서버 종료 시 미래 예약을 실행하지 않고 취소한다")
    void discardDelayedTaskOnShutdown() {
        var scheduler = new NotificationRetryConfiguration().notificationRetryScheduler(Clock.systemUTC());
        scheduler.initialize();
        var ran = new AtomicBoolean();
        var pending = scheduler.schedule(() -> ran.set(true), Instant.now().plusSeconds(60));

        scheduler.shutdown();

        assertThat(pending.isCancelled()).isTrue();
        assertThat(ran).isFalse();
        assertThat(scheduler.getScheduledExecutor().isShutdown()).isTrue();
    }
}
