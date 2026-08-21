package yeobaek.backend.admin.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.AuthorEntryRequest;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.dto.ChapterUploadRequest;
import yeobaek.backend.admin.dto.PassageUploadRequest;
import yeobaek.backend.admin.service.BookIngestService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@WebMvcTest(AdminBookController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class AdminBookControllerTest extends ControllerTest {

    @MockitoBean
    private BookIngestService bookIngestService;

    @Test
    @DisplayName("도서 업로드 JSON 전체를 서비스에 전달하고 생성 응답 계약을 반환한다")
    void uploadBook() throws Exception {
        var request = new BookUploadRequest(
                "운수 좋은 날",
                null,
                1924,
                List.of(
                        new AuthorEntryRequest(null, "현진건", "0000 0001 2345 964X"),
                        new AuthorEntryRequest(12L, null, null)),
                List.of(
                        new ChapterUploadRequest("1장", List.of(
                                new PassageUploadRequest("첫 본문"),
                                new PassageUploadRequest("둘째 본문"))),
                        new ChapterUploadRequest("2장", List.of(
                                new PassageUploadRequest("셋째 본문")))));
        var response = new BookUploadResponse(3L, "운수 좋은 날", 3);
        given(bookIngestService.upload(request)).willReturn(response);

        mockMvc.perform(post("/api/admin/books")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "운수 좋은 날",
                                  "publisher": null,
                                  "publishedYear": 1924,
                                  "authors": [
                                    {"name": "현진건", "isni": "0000 0001 2345 964X"},
                                    {"authorId": 12}
                                  ],
                                  "chapters": [
                                    {
                                      "title": "1장",
                                      "passages": [
                                        {"content": "첫 본문"},
                                        {"content": "둘째 본문"}
                                      ]
                                    },
                                    {
                                      "title": "2장",
                                      "passages": [{"content": "셋째 본문"}]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookId").value(3))
                .andExpect(jsonPath("$.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.passageCount").value(3));

        verify(bookIngestService, times(1)).upload(request);
    }

    @Test
    @DisplayName("도서 업로드 본문이 없으면 서비스를 호출하지 않는다")
    void rejectMissingBody() throws Exception {
        mockMvc.perform(post("/api/admin/books")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertInstanceOf(
                        HttpMessageNotReadableException.class,
                        result.getResolvedException()));

        verifyNoInteractions(bookIngestService);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        var request = new BookUploadRequest(
                "새 도서",
                "출판사",
                2026,
                List.of(new AuthorEntryRequest(999L, null, null)),
                List.of(new ChapterUploadRequest(
                        "1장",
                        List.of(new PassageUploadRequest("본문")))));
        var serviceException = new NotFoundException(ErrorCode.AUTHOR_NOT_FOUND);
        given(bookIngestService.upload(request)).willThrow(serviceException);

        var result = mockMvc.perform(post("/api/admin/books")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"새 도서",
                                  "publisher":"출판사",
                                  "publishedYear":2026,
                                  "authors":[{"authorId":999}],
                                  "chapters":[{"title":"1장","passages":[{"content":"본문"}]}]
                                }
                                """))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(bookIngestService, times(1)).upload(request);
    }
}
