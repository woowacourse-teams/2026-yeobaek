package yeobaek.backend.support.notification;

import java.time.Clock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.FcmRetryService;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

@Configuration(proxyBeanMethods = false)
public class NotificationRetryConfiguration {

    @Bean(defaultCandidate = false)
    @Qualifier("notificationRetryClock")
    public Clock notificationRetryClock() {
        return Clock.systemUTC();
    }

    @Bean(defaultCandidate = false)
    @Qualifier("notificationRetryScheduler")
    public ThreadPoolTaskScheduler notificationRetryScheduler(
            @Qualifier("notificationRetryClock") Clock clock) {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("notification-retry-timer-");
        scheduler.setClock(clock);
        scheduler.setTaskDecorator(new NotificationTaskDecorator());
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return scheduler;
    }

    @Bean
    public FcmRetryService fcmRetryService(
            @Qualifier("notificationRetryScheduler") TaskScheduler scheduler,
            @Qualifier("notificationTaskExecutor") TaskExecutor executor,
            @Qualifier("notificationRetryClock") Clock clock,
            FcmPushSender sender, NotificationRegistrationService registrationService, FcmFailureHandler failureHandler,
            @Value("${notification.retry.pending-capacity:1000}") int pendingCapacity) {
        if (pendingCapacity <= 0) {
            throw new IllegalArgumentException("재시도 예약 대기 용량은 1 이상이어야 합니다.");
        }
        return new FcmRetryService(scheduler, executor, clock, sender, registrationService, failureHandler,
                new Semaphore(pendingCapacity));
    }
}
