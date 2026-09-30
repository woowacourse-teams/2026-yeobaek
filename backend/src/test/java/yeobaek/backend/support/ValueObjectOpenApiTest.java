package yeobaek.backend.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ValueObjectOpenApiTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("VO 요청 필드는 OpenAPI에서도 기존 문자열 타입으로 노출한다")
    void requestSchemasRemainStrings() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.MemberCreateRequest.properties.nickname.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.ClubCreateRequest.properties.name.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.ClubJoinRequest.properties.joinCode.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.CommentCreateRequest.properties.content.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.CommentUpdateRequest.properties.content.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.BookUploadRequest.properties.title.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.ChapterUploadRequest.properties.title.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.SentenceUploadRequest.properties.content.type").value("string"));
    }
}
