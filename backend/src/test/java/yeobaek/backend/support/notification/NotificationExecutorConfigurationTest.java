package yeobaek.backend.support.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class NotificationExecutorConfigurationTest {

    @Test
    @DisplayName("알림 전용 실행기는 설정값으로 제한하고 Boot 기본 실행기를 대체하지 않는다")
    void retainDefaultExecutorAndBindNotificationLimits() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
                .withUserConfiguration(NotificationExecutorConfiguration.class)
                .withPropertyValues("notification.executor.pool-size=1", "notification.executor.queue-capacity=3")
                .run(context -> {
                    assertThat(context).hasBean("applicationTaskExecutor");
                    var notification = context.getBean("notificationTaskExecutor", ThreadPoolTaskExecutor.class);
                    assertThat(notification.getCorePoolSize()).isEqualTo(1);
                    assertThat(notification.getMaxPoolSize()).isEqualTo(1);
                    assertThat(notification.getQueueCapacity()).isEqualTo(3);
                });
    }
}
