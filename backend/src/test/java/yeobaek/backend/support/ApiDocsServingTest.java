package yeobaek.backend.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ApiDocsServingTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OpenAPI 스펙이 서빙된다")
    void serveOpenApiSpec() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/admin/books/{bookId}']['delete']").exists())
                .andExpect(jsonPath("$['paths']['/api/members/me']['delete']").exists())
                .andExpect(jsonPath("$['paths']['/api/members/me/blocks']['get']").exists())
                .andExpect(jsonPath("$['paths']['/api/members/me/blocks/{memberId}']['put']").exists())
                .andExpect(jsonPath("$['paths']['/api/members/me/blocks/{memberId}']['delete']").exists())
                .andExpect(jsonPath("$['paths']['/api/comments/{commentId}/reports']['post']").exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/comments/{commentId}/reports']['post']['responses']['204']")
                        .exists())
                .andExpect(jsonPath(
                        "$['paths']['/api/comments/{commentId}/reports']['post']['parameters'][0]['name']")
                        .value("commentId"))
                .andExpect(jsonPath(
                        "$['paths']['/api/comments/{commentId}/reports']['post']['parameters'][0]['in']")
                        .value("path"))
                .andExpect(jsonPath(
                        "$['paths']['/api/comments/{commentId}/reports']['post']['parameters'][0]['required']")
                        .value(true))
                .andExpect(jsonPath(
                        "$['paths']['/api/comments/{commentId}/reports']['post']['requestBody']")
                        .doesNotExist())
                .andExpect(jsonPath(
                        "$['components']['schemas']['ClubMemberResponse']['properties']['blocked']['type']")
                        .value("boolean"))
                .andExpect(jsonPath(
                        "$['components']['schemas']['ClubBookResponse']['properties']['status']['enum'][0]")
                        .value("ACTIVE"))
                .andExpect(jsonPath(
                        "$['components']['schemas']['ClubBookResponse']['properties']['status']['enum'][1]")
                        .value("DELETED"))
                .andExpect(jsonPath(
                        "$['components']['schemas']['AdminAuthorBookResponse']['properties']['status']['enum'][0]")
                        .value("ACTIVE"))
                .andExpect(jsonPath(
                        "$['components']['schemas']['AdminAuthorBookResponse']['properties']['status']['enum'][1]")
                        .value("DELETED"));
    }

    @Test
    @DisplayName("/docs 진입 시 Swagger UI로 리다이렉트된다")
    void redirectDocsToSwaggerUi() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection());
    }
}
