package watson.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import watson.backend.support.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
