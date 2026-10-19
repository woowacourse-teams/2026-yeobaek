package yeobaek.backend.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.bean.override.convention.TestBean;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    // 업무 통합 테스트는 알림 작업까지 마친 뒤 DB를 정리한다. 비동기 동작은 전용 테스트에서 검증한다.
    @TestBean(name = "notificationTaskExecutor", methodName = "synchronousNotificationExecutor")
    private TaskExecutor notificationExecutor;

    public static TaskExecutor synchronousNotificationExecutor() {
        return new SyncTaskExecutor();
    }

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }
}
