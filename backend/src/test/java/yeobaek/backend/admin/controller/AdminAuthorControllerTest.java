package yeobaek.backend.admin.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.AdminAuthorBookResponse;
import yeobaek.backend.admin.dto.AdminAuthorResponse;
import yeobaek.backend.admin.dto.AdminAuthorsResponse;
import yeobaek.backend.admin.service.AdminAuthorService;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(AdminAuthorController.class)
@TestPropertySource(properties = "admin.token=test-admin-token")
class AdminAuthorControllerTest extends ControllerTest {

    @MockitoBean
    private AdminAuthorService adminAuthorService;

    @Test
    @DisplayName("관리자 토큰으로 작가 목록을 조회한다")
    void findAuthors() throws Exception {
        given(adminAuthorService.findAuthors()).willReturn(new AdminAuthorsResponse(List.of(
                new AdminAuthorResponse(12L, "현진건", "000000012345964X",
                        List.of(new AdminAuthorBookResponse(3L, "운수 좋은 날"))))));

        mockMvc.perform(get("/api/admin/authors").header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authors[0].authorId").value(12))
                .andExpect(jsonPath("$.authors[0].books[0].title").value("운수 좋은 날"))
                .andDo(document("admin-author-list", resource(ResourceSnippetParameters.builder()
                        .tag("관리자")
                        .summary("작가 목록 조회")
                        .description("업로드 전 기존 작가 확인용. 등록순, 페이징 없음.")
                        .requestHeaders(headerWithName("X-Admin-Token").description("고정 관리자 토큰"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("authors[].authorId").description("작가 ID"),
                                PayloadDocumentation.fieldWithPath("authors[].name").description("이름"),
                                PayloadDocumentation.fieldWithPath("authors[].isni").description("정규화된 ISNI (없으면 null)").optional(),
                                PayloadDocumentation.fieldWithPath("authors[].books[].bookId").description("작품 도서 ID"),
                                PayloadDocumentation.fieldWithPath("authors[].books[].title").description("작품 제목"))
                        .build())));
    }

    @Test
    @DisplayName("관리자 토큰이 없으면 401을 응답한다")
    void rejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/admin/authors"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
