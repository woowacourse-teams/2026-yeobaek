package yeobaek.backend.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class DatabaseCleaningIntegrationTest extends IntegrationTest {

    @Autowired
    private DatabaseCleaner databaseCleaner;

    // 커밋된 데이터를 다음 테스트가 공통 DB에서 관찰하지 않도록 종료 후 정리한다.
    @AfterEach
    void cleanCommittedData() {
        databaseCleaner.clean();
    }
}
