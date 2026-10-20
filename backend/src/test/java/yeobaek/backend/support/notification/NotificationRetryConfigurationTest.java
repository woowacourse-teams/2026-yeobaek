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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.FcmRetryProcessor;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

class NotificationRetryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationRetryConfiguration.class)
            .withBean("notificationTaskExecutor", TaskExecutor.class, SyncTaskExecutor::new)
            .withBean(FcmPushSender.class, () -> mock(FcmPushSender.class))
            .withBean(FcmFailureHandler.class, () -> mock(FcmFailureHandler.class))
            .withBean(NotificationRegistrationService.class, () -> mock(NotificationRegistrationService.class));

    @Test
    @DisplayName("재시도 예약기는 전용 시계와 단일 타이머를 쓰고 종료 시 지연 작업을 폐기한다")
    void configureRetryBeansAndShutdownPolicy() {
        contextRunner.withPropertyValues("notification.retry.pending-capacity=2").run(context -> {
            assertThat(context).hasSingleBean(FcmRetryProcessor.class);
            var scheduler = context.getBean("notificationRetryScheduler", ThreadPoolTaskScheduler.class);
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
            assertThat(scheduler.getClock()).isSameAs(context.getBean("notificationRetryClock", Clock.class));
            assertThat(scheduler.getScheduledThreadPoolExecutor().getExecuteExistingDelayedTasksAfterShutdownPolicy())
                    .isFalse();
        });
    }

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
