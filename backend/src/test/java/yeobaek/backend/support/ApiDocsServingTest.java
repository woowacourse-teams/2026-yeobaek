package yeobaek.backend.support;

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
    @DisplayName("OpenAPI 스펙이 서빙된다")
    void serveOpenApiSpec() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/docs 진입 시 Swagger UI로 리다이렉트된다")
    void redirectDocsToSwaggerUi() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection());
    }
}
