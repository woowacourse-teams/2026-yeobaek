package yeobaek.backend.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.service.BookIngestService;
import yeobaek.backend.support.ControllerTest;

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
                .andExpect(jsonPath("$.passageCount").value(1));
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
