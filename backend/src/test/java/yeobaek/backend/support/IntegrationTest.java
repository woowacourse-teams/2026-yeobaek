package yeobaek.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.notification.FcmRetryService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    // 업무 통합 테스트는 알림 작업을 동기 실행한다. 비동기 동작은 전용 테스트에서 검증한다.
    @TestBean(name = "notificationTaskExecutor", methodName = "synchronousNotificationExecutor")
    private TaskExecutor notificationExecutor;

    public static TaskExecutor synchronousNotificationExecutor() {
        return new SyncTaskExecutor();
    }

    // 예약·반복 발송은 전용 테스트에서 검증하고, 업무 테스트 사이에 지연 작업을 남기지 않는다.
    @MockitoBean
    private FcmRetryService retryService;
}
