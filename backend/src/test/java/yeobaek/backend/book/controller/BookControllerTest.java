package yeobaek.backend.book.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import yeobaek.backend.book.dto.BookDetailResponse;
import yeobaek.backend.book.dto.BookSummaryResponse;
import yeobaek.backend.book.dto.BooksResponse;
import yeobaek.backend.book.dto.ChapterResponse;
import yeobaek.backend.book.service.BookService;
import yeobaek.backend.support.ControllerTest;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@WebMvcTest(BookController.class)
class BookControllerTest extends ControllerTest {

    @MockitoBean
    private BookService bookService;

    @Test
    @DisplayName("검색어가 없으면 null을 서비스에 전달하고 도서 목록 전체 계약을 반환한다")
    void findBooksWithoutKeyword() throws Exception {
        givenValidMember(1L);
        var response = new BooksResponse(List.of(
                new BookSummaryResponse(1L, "운수 좋은 날", List.of("현진건"),
                        "자체 제작", 1924, "https://covers.example/cover.jpg", 312),
                new BookSummaryResponse(2L, "작자 미상 작품", List.of(), null, null, null, 20)));
        given(bookService.findBooks(null)).willReturn(response);

        mockMvc.perform(get("/api/books")
                        .header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books.length()").value(2))
                .andExpect(jsonPath("$.books[0].bookId").value(1))
                .andExpect(jsonPath("$.books[0].title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.books[0].authors").isArray())
                .andExpect(jsonPath("$.books[0].authors.length()").value(1))
                .andExpect(jsonPath("$.books[0].authors[0]").value("현진건"))
                .andExpect(jsonPath("$.books[0].publisher").value("자체 제작"))
                .andExpect(jsonPath("$.books[0].publishedYear").value(1924))
                .andExpect(jsonPath("$.books[0].coverImageUrl").value("https://covers.example/cover.jpg"))
                .andExpect(jsonPath("$.books[0].passageCount").value(312))
                .andExpect(jsonPath("$.books[1].bookId").value(2))
                .andExpect(jsonPath("$.books[1].title").value("작자 미상 작품"))
                .andExpect(jsonPath("$.books[1].authors").isArray())
                .andExpect(jsonPath("$.books[1].authors.length()").value(0))
                .andExpect(jsonPath("$.books[1].publisher").value((Object) null))
                .andExpect(jsonPath("$.books[1].publishedYear").value((Object) null))
                .andExpect(jsonPath("$.books[1].coverImageUrl").value((Object) null))
                .andExpect(jsonPath("$.books[1].passageCount").value(20));

        verify(bookService, times(1)).findBooks(null);
    }

    @Test
    @DisplayName("검색어를 서비스에 그대로 전달한다")
    void findBooksWithKeyword() throws Exception {
        givenValidMember(2L);
        given(bookService.findBooks("현진건")).willReturn(new BooksResponse(List.of()));

        mockMvc.perform(get("/api/books")
                        .header("X-Member-Id", "2")
                        .param("keyword", "현진건"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books.length()").value(0));

        verify(bookService, times(1)).findBooks("현진건");
    }

    @Test
    @DisplayName("도서 식별자를 서비스에 전달하고 상세와 목차 전체 계약을 반환한다")
    void findBook() throws Exception {
        givenValidMember(3L);
        var response = new BookDetailResponse(
                9L,
                "운수 좋은 날",
                List.of("현진건", "공동 저자"),
                null,
                null,
                "https://covers.example/detail.jpg",
                312,
                List.of(new ChapterResponse(11L, "1장", 1, 1, 105)));
        given(bookService.findBook(9L)).willReturn(response);

        mockMvc.perform(get("/api/books/{bookId}", 9L)
                        .header("X-Member-Id", "3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookId").value(9))
                .andExpect(jsonPath("$.title").value("운수 좋은 날"))
                .andExpect(jsonPath("$.authors").isArray())
                .andExpect(jsonPath("$.authors.length()").value(2))
                .andExpect(jsonPath("$.authors[0]").value("현진건"))
                .andExpect(jsonPath("$.authors[1]").value("공동 저자"))
                .andExpect(jsonPath("$.publisher").value((Object) null))
                .andExpect(jsonPath("$.publishedYear").value((Object) null))
                .andExpect(jsonPath("$.coverImageUrl").value("https://covers.example/detail.jpg"))
                .andExpect(jsonPath("$.passageCount").value(312))
                .andExpect(jsonPath("$.chapters").isArray())
                .andExpect(jsonPath("$.chapters.length()").value(1))
                .andExpect(jsonPath("$.chapters[0].chapterId").value(11))
                .andExpect(jsonPath("$.chapters[0].title").value("1장"))
                .andExpect(jsonPath("$.chapters[0].sequence").value(1))
                .andExpect(jsonPath("$.chapters[0].startPassageSequence").value(1))
                .andExpect(jsonPath("$.chapters[0].endPassageSequence").value(105));

        verify(bookService, times(1)).findBook(9L);
    }

    @Test
    @DisplayName("도서 식별자의 타입이 올바르지 않으면 서비스를 호출하지 않는다")
    void rejectInvalidBookIdType() throws Exception {
        givenValidMember(4L);

        mockMvc.perform(get("/api/books/{bookId}", "invalid")
                        .header("X-Member-Id", "4"))
                .andExpect(result -> assertInstanceOf(
                        MethodArgumentTypeMismatchException.class,
                        result.getResolvedException()));

        verifyNoInteractions(bookService);
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        givenValidMember(5L);
        var serviceException = new NotFoundException(ErrorCode.BOOK_NOT_FOUND);
        given(bookService.findBook(999L)).willThrow(serviceException);

        var result = mockMvc.perform(get("/api/books/{bookId}", 999L)
                        .header("X-Member-Id", "5"))
                .andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 서비스 예외 인스턴스를 변경하지 않아야 한다");
        verify(bookService, times(1)).findBook(999L);
    }
}
