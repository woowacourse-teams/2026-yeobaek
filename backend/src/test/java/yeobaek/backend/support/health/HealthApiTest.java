package yeobaek.backend.support.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import yeobaek.backend.support.IntegrationTest;

@TestPropertySource(properties = "DEPLOYED_COMMIT_SHA=9728d096b540d8ad13d856e91c44795b21ab4113")
class HealthApiTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("실제 데이터베이스에 SELECT 1을 실행해 서버와 데이터베이스 상태를 확인한다")
    void healthCheckWithDatabase() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deployed-Commit-SHA", "9728d096b540d8ad13d856e91c44795b21ab4113"))
                .andExpect(content().string(""));
    }
}
