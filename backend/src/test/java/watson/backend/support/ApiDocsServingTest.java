package watson.backend.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiDocsServingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger UI 문서 페이지가 서빙된다")
    void serveSwaggerUiPage() throws Exception {
        mockMvc.perform(get("/docs/index.html"))
                .andExpect(status().isOk());
    }
}
