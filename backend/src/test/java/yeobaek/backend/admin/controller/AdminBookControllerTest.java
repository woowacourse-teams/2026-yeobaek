package yeobaek.backend.admin.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.admin.dto.AuthorEntryRequest;
import yeobaek.backend.admin.dto.AdminBookAuthorResponse;
import yeobaek.backend.admin.dto.AdminBookResponse;
import yeobaek.backend.admin.dto.AdminBooksResponse;
import yeobaek.backend.admin.dto.BookUploadRequest;
import yeobaek.backend.admin.dto.BookUploadResponse;
import yeobaek.backend.admin.dto.ChapterUploadRequest;
import yeobaek.backend.admin.dto.PassageUploadRequest;
import yeobaek.backend.admin.dto.SentenceUploadRequest;
import yeobaek.backend.admin.service.AdminBookService;
import yeobaek.backend.admin.service.BookIngestService;
import yeobaek.backend.book.domain.BookStatus;
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
    @DisplayName("업로드된 도서 목록의 전체 응답 계약을 반환한다")
    void findBooks() throws Exception {
        var response = new AdminBooksResponse(List.of(
                new AdminBookResponse(
                        3L,
                        "함께 쓴 책",
                        List.of(
                                new AdminBookAuthorResponse(7L, "첫 작가", "000000012345964X"),
                                new AdminBookAuthorResponse(8L, "둘째 작가", null)),
                        "여백 출판",
                        2026,
                        42,
                        "https://covers.example/books/3.jpg",
                        BookStatus.ACTIVE),
                new AdminBookResponse(
                        4L,
                        "삭제된 책",
                        List.of(),
                        null,
                        null,
                        0,
                        null,
                        BookStatus.DELETED)));
        given(adminBookService.findBooks()).willReturn(response);

        mockMvc.perform(get("/api/admin/books")
                        .header("X-Admin-Token", "controller-test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books.length()").value(2))
                .andExpect(jsonPath("$.books[0].bookId").value(3))
                .andExpect(jsonPath("$.books[0].title").value("함께 쓴 책"))
                .andExpect(jsonPath("$.books[0].authors").isArray())
                .andExpect(jsonPath("$.books[0].authors.length()").value(2))
                .andExpect(jsonPath("$.books[0].authors[0].authorId").value(7))
                .andExpect(jsonPath("$.books[0].authors[0].name").value("첫 작가"))
                .andExpect(jsonPath("$.books[0].authors[0].isni").value("000000012345964X"))
                .andExpect(jsonPath("$.books[0].authors[1].authorId").value(8))
                .andExpect(jsonPath("$.books[0].authors[1].name").value("둘째 작가"))
                .andExpect(jsonPath("$.books[0].authors[1].isni").value((Object) null))
                .andExpect(jsonPath("$.books[0].publisher").value("여백 출판"))
                .andExpect(jsonPath("$.books[0].publishedYear").value(2026))
                .andExpect(jsonPath("$.books[0].passageCount").value(42))
                .andExpect(jsonPath("$.books[0].coverImageUrl").value("https://covers.example/books/3.jpg"))
                .andExpect(jsonPath("$.books[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.books[1].bookId").value(4))
                .andExpect(jsonPath("$.books[1].title").value("삭제된 책"))
                .andExpect(jsonPath("$.books[1].authors").isEmpty())
                .andExpect(jsonPath("$.books[1].publisher").value((Object) null))
                .andExpect(jsonPath("$.books[1].publishedYear").value((Object) null))
                .andExpect(jsonPath("$.books[1].passageCount").value(0))
                .andExpect(jsonPath("$.books[1].coverImageUrl").value((Object) null))
                .andExpect(jsonPath("$.books[1].status").value("DELETED"));

        verify(adminBookService, times(1)).findBooks();
    }

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

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"coverImageKey\":null}"})
    @DisplayName("필수 표지 키가 누락되거나 null이면 서비스를 호출하지 않는다")
    void rejectMissingOrNullCoverImageKey(String content) throws Exception {
        mockMvc.perform(put("/api/admin/books/{bookId}/cover", 3L)
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));

        verifyNoInteractions(adminBookService);
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
        willThrow(new BadRequestException(
                ErrorCode.BOOK_NOT_AVAILABLE,
                "더 이상 이용할 수 없는 도서입니다."))
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
                                passage("첫 본문"),
                                passage("둘째 본문"))),
                        new ChapterUploadRequest("2장", List.of(
                                passage("셋째 본문")))));
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
                                        {"sentences": [{"content": "첫 본문"}]},
                                        {"sentences": [{"content": "둘째 본문"}]}
                                      ]
                                    },
                                    {
                                      "title": "2장",
                                      "passages": [{"sentences": [{"content": "셋째 본문"}]}]
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequiredUploadFields")
    @DisplayName("도서 업로드 필수 필드가 누락되거나 null이면 서비스를 호출하지 않는다")
    void rejectMissingOrNullUploadField(String description, String content) throws Exception {
        mockMvc.perform(post("/api/admin/books")
                        .header("X-Admin-Token", "controller-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException()));

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
                        List.of(passage("본문")))));
        var serviceException = new NotFoundException(
                ErrorCode.AUTHOR_NOT_FOUND,
                "authorId가 가리키는 작가가 존재하지 않습니다: authorId=99");
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
                                  "chapters":[{"title":"1장","passages":[{"sentences":[{"content":"본문"}]}]}]
                                }
                                """))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(bookIngestService, times(1)).upload(request);
    }

    private PassageUploadRequest passage(String content) {
        return new PassageUploadRequest(List.of(new SentenceUploadRequest(content)));
    }

    private static Stream<Arguments> invalidRequiredUploadFields() {
        return Stream.of(
                Arguments.of("title 누락", "{\"authors\":[],\"chapters\":[]}"),
                Arguments.of("title null", "{\"title\":null,\"authors\":[],\"chapters\":[]}"),
                Arguments.of("authors 누락", "{\"title\":\"책\",\"chapters\":[]}"),
                Arguments.of("authors null", "{\"title\":\"책\",\"authors\":null,\"chapters\":[]}"),
                Arguments.of("authors null 원소", "{\"title\":\"책\",\"authors\":[null],\"chapters\":[]}"),
                Arguments.of("author name 누락", "{\"title\":\"책\",\"authors\":[{}],\"chapters\":[]}"),
                Arguments.of("author name null", "{\"title\":\"책\",\"authors\":[{\"name\":null}],\"chapters\":[]}"),
                Arguments.of("ISNI 작가 name 누락",
                        "{\"title\":\"책\",\"authors\":[{\"isni\":\"000000012345964X\"}],\"chapters\":[]}"),
                Arguments.of("ISNI 작가 name null",
                        "{\"title\":\"책\",\"authors\":[{\"name\":null,\"isni\":\"000000012345964X\"}],\"chapters\":[]}"),
                Arguments.of("chapters 누락", "{\"title\":\"책\",\"authors\":[]}"),
                Arguments.of("chapters null", "{\"title\":\"책\",\"authors\":[],\"chapters\":null}"),
                Arguments.of("chapters null 원소", "{\"title\":\"책\",\"authors\":[],\"chapters\":[null]}"),
                Arguments.of("chapter title 누락", validUploadWithChapter("\"passages\":[]")),
                Arguments.of("chapter title null", validUploadWithChapter("\"title\":null,\"passages\":[]")),
                Arguments.of("passages 누락", validUploadWithChapter("\"title\":\"1장\"")),
                Arguments.of("passages null", validUploadWithChapter("\"title\":\"1장\",\"passages\":null")),
                Arguments.of("passages null 원소", validUploadWithChapter("\"title\":\"1장\",\"passages\":[null]")),
                Arguments.of("sentences 누락", validUploadWithPassage("")),
                Arguments.of("sentences null", validUploadWithPassage("\"sentences\":null")),
                Arguments.of("sentences null 원소", validUploadWithPassage("\"sentences\":[null]")),
                Arguments.of("sentence content 누락", validUploadWithPassage("\"sentences\":[{}]")),
                Arguments.of("sentence content null", validUploadWithPassage("\"sentences\":[{\"content\":null}]")));
    }

    private static String validUploadWithChapter(String chapterFields) {
        return "{\"title\":\"책\",\"authors\":[],\"chapters\":[{" + chapterFields + "}]}";
    }

    private static String validUploadWithPassage(String passageFields) {
        return validUploadWithChapter("\"title\":\"1장\",\"passages\":[{" + passageFields + "}]");
    }
}
