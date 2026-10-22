package yeobaek.backend.support.notification;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class NotificationExecutorConfiguration {

    @Bean(defaultCandidate = false)
    @Qualifier("notificationTaskExecutor")
    public ThreadPoolTaskExecutor notificationTaskExecutor(
            @Value("${notification.executor.pool-size:2}") int poolSize,
            @Value("${notification.executor.queue-capacity:100}") int queueCapacity) {
        if (poolSize < 1 || queueCapacity < 0) {
            throw new IllegalArgumentException("알림 워커는 1개 이상, 대기 용량은 0 이상이어야 합니다.");
        }
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("notification-");
        executor.setTaskDecorator(new NotificationTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setStrictEarlyShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
