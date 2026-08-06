package watson.backend.admin.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import watson.backend.admin.dto.BookUploadRequest;
import watson.backend.admin.dto.BookUploadResponse;
import watson.backend.admin.service.BookIngestService;
import watson.backend.support.ControllerTest;

@WebMvcTest(AdminBookController.class)
@TestPropertySource(properties = "admin.token=test-admin-token")
class AdminBookControllerTest extends ControllerTest {

    private static final String UPLOAD_BODY = """
            {
              "title": "운수 좋은 날",
              "publisher": "자체 제작",
              "publishedYear": 1924,
              "authors": [{ "name": "현진건", "isni": "0000 0001 2345 964X" }],
              "chapters": [{ "title": "1장", "passages": [{ "content": "첫 문단" }] }]
            }
            """;

    @MockitoBean
    private BookIngestService bookIngestService;

    @Test
    @DisplayName("관리자 토큰으로 도서를 업로드하면 201을 응답한다")
    void upload() throws Exception {
        given(bookIngestService.upload(any(BookUploadRequest.class)))
                .willReturn(new BookUploadResponse(3L, "운수 좋은 날", 1));

        mockMvc.perform(post("/api/admin/books")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPLOAD_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(3))
                .andExpect(jsonPath("$.passageCount").value(1))
                .andDo(document("admin-book-upload", resource(ResourceSnippetParameters.builder()
                        .tag("관리자")
                        .summary("도서 업로드 (인제스트 규격 JSON)")
                        .description("본문 순서는 배열 등장 순서로 서버가 1..N을 부여한다. 앱은 사용하지 않는다.")
                        .requestHeaders(headerWithName("X-Admin-Token").description("고정 관리자 토큰"))
                        .requestFields(
                                PayloadDocumentation.fieldWithPath("title").description("도서 제목 (1~100자)"),
                                PayloadDocumentation.fieldWithPath("publisher").description("출판사 (선택, 최대 100자)").optional(),
                                PayloadDocumentation.fieldWithPath("publishedYear").description("출판연도 (선택, 정수)").optional(),
                                PayloadDocumentation.fieldWithPath("authors[].name").description("작가 이름 (신규·ISNI 형태)").optional(),
                                PayloadDocumentation.fieldWithPath("authors[].isni").description("ISNI (선택)").optional(),
                                PayloadDocumentation.fieldWithPath("authors[].authorId").description("기존 작가 참조 (선택)")
                                        .type(JsonFieldType.NUMBER).optional(),
                                PayloadDocumentation.fieldWithPath("chapters[].title").description("목차 제목 (1~100자)"),
                                PayloadDocumentation.fieldWithPath("chapters[].passages[].content").description("본문 내용 (필수)"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("bookId").description("생성된 도서 ID"),
                                PayloadDocumentation.fieldWithPath("title").description("제목"),
                                PayloadDocumentation.fieldWithPath("passageCount").description("자동 산출된 본문 개수"))
                        .build())));
    }

    @Test
    @DisplayName("관리자 토큰이 없으면 401을 응답한다")
    void rejectMissingToken() throws Exception {
        mockMvc.perform(post("/api/admin/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPLOAD_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("관리자 토큰이 일치하지 않으면 401을 응답한다")
    void rejectWrongToken() throws Exception {
        mockMvc.perform(post("/api/admin/books")
                        .header("X-Admin-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPLOAD_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
