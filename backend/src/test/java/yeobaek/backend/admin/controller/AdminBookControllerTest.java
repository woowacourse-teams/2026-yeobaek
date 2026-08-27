package yeobaek.backend.admin.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import yeobaek.backend.admin.service.AdminBookService;
import yeobaek.backend.admin.service.BookIngestService;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@WebMvcTest(AdminBookController.class)
@TestPropertySource(properties = "admin.token=controller-test-token")
class AdminBookControllerTest extends ControllerTest {

    @MockitoBean
    private BookIngestService bookIngestService;

    @MockitoBean
    private AdminBookService adminBookService;

    @Test
    @DisplayName("도서 삭제 요청의 ID를 서비스에 전달하고 빈 204 응답을 반환한다")
    void deleteBook() throws Exception {
        mockMvc.perform(delete("/api/admin/books/{bookId}", 3L)
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(adminBookService, times(1)).delete(3L);
    }

    @Test
    @DisplayName("도서 표지 교체 요청의 ID와 키를 서비스에 전달하고 204를 반환한다")
    void replaceCoverImage() throws Exception {
        String key = "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.webp";

        mockMvc.perform(put("/api/admin/books/{bookId}/cover", 3L)
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"coverImageKey":"yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.webp"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(adminBookService).replaceCoverImage(3L, key);
    }

    @Test
    @DisplayName("도서 표지 제거 요청의 ID를 서비스에 전달하고 204를 반환한다")
    void removeCoverImage() throws Exception {
        mockMvc.perform(delete("/api/admin/books/{bookId}/cover", 3L)
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(adminBookService).removeCoverImage(3L);
    }

    @Test
    @DisplayName("이미 삭제된 도서의 삭제 요청은 이용 불가 오류를 반환한다")
    void rejectAlreadyDeletedBook() throws Exception {
        willThrow(new BadRequestException(ErrorCode.BOOK_NOT_AVAILABLE))
                .given(adminBookService).delete(3L);

        mockMvc.perform(delete("/api/admin/books/{bookId}", 3L)
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.message").value("더 이상 이용할 수 없는 도서입니다."));

        verify(adminBookService, times(1)).delete(3L);
    }

    @Test
    @DisplayName("도서 업로드 JSON 전체를 서비스에 전달하고 생성 응답 계약을 반환한다")
    void uploadBook() throws Exception {
        var request = new BookUploadRequest(
                "운수 좋은 날",
                null,
                1924,
                "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.jpg",
                List.of(
                        new AuthorEntryRequest(null, "현진건", "0000 0001 2345 964X"),
                        new AuthorEntryRequest(12L, null, null)),
                List.of(
                        new ChapterUploadRequest("1장", List.of(
                                new PassageUploadRequest("첫 본문"),
                                new PassageUploadRequest("둘째 본문"))),
                        new ChapterUploadRequest("2장", List.of(
                                new PassageUploadRequest("셋째 본문")))));
        var response = new BookUploadResponse(3L, "운수 좋은 날",
                "https://covers.example/yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.jpg", 3);
        given(bookIngestService.upload(request)).willReturn(response);

        mockMvc.perform(post("/api/admin/books")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "운수 좋은 날",
                                  "publisher": null,
                                  "publishedYear": 1924,
                                  "coverImageKey": "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.jpg",
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
                .andExpect(jsonPath("$.coverImageUrl").value(response.coverImageUrl()))
                .andExpect(jsonPath("$.passageCount").value(3));

        verify(bookIngestService, times(1)).upload(request);
    }

    @Test
    @DisplayName("표지가 없는 도서 업로드 응답은 coverImageUrl을 null로 포함한다")
    void uploadBookWithoutCover() throws Exception {
        var request = new BookUploadRequest("표지 없는 책", null, null, null, List.of(), List.of());
        given(bookIngestService.upload(request))
                .willReturn(new BookUploadResponse(4L, "표지 없는 책", null, 0));

        mockMvc.perform(post("/api/admin/books")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"표지 없는 책","authors":[],"chapters":[]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverImageUrl").value((Object) null));

        verify(bookIngestService).upload(request);
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
                null,
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
