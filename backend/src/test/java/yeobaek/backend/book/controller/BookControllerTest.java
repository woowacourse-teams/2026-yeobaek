package yeobaek.backend.book.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
    @DisplayName("도서 목록을 조회한다")
    void findBooks() throws Exception {
        givenValidMember(1L);
        given(bookService.findBooks(null)).willReturn(new BooksResponse(List.of(
                new BookSummaryResponse(1L, "운수 좋은 날", List.of("현진건"), "자체 제작", 1924, 312))));

        mockMvc.perform(get("/api/books").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].bookId").value(1))
                .andExpect(jsonPath("$.books[0].authors[0]").value("현진건"));
    }

    @Test
    @DisplayName("키워드로 도서를 검색한다")
    void searchBooks() throws Exception {
        givenValidMember(1L);
        given(bookService.findBooks("현진건")).willReturn(new BooksResponse(List.of(
                new BookSummaryResponse(1L, "운수 좋은 날", List.of("현진건"), "자체 제작", 1924, 312))));

        mockMvc.perform(get("/api/books").param("keyword", "현진건").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].title").value("운수 좋은 날"));
    }

    @Test
    @DisplayName("도서 상세와 목차를 조회한다")
    void findBook() throws Exception {
        givenValidMember(1L);
        given(bookService.findBook(1L)).willReturn(new BookDetailResponse(1L, "운수 좋은 날", List.of("현진건"),
                "자체 제작", 1924, 312, List.of(new ChapterResponse(1L, "1장", 1, 1, 105))));

        mockMvc.perform(get("/api/books/{bookId}", 1L).header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].startPassageSequence").value(1))
                .andExpect(jsonPath("$.chapters[0].endPassageSequence").value(105));
    }

    @Test
    @DisplayName("서비스 예외를 변경하지 않고 전파한다")
    void propagateServiceException() throws Exception {
        givenValidMember(1L);
        var serviceException = new NotFoundException(ErrorCode.BOOK_NOT_FOUND);
        given(bookService.findBook(999L)).willThrow(serviceException);

        mockMvc.perform(get("/api/books/{bookId}", 999L)
                        .header("X-Member-Id", "1"))
                .andExpect(result -> assertSame(
                        serviceException,
                        result.getResolvedException(),
                        "컨트롤러는 서비스 예외의 동일한 인스턴스를 전파해야 한다"));

        verify(bookService, times(1)).findBook(999L);
    }

    @Test
    @DisplayName("X-Member-Id 헤더가 없으면 400을 응답한다")
    void missingMemberIdHeader() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("X-Member-Id 헤더가 필요합니다."));
    }

    @Test
    @DisplayName("존재하지 않는 회원의 요청은 400을 응답한다")
    void unknownMember() throws Exception {
        given(memberRepository.existsById(99L)).willReturn(false);

        mockMvc.perform(get("/api/books").header("X-Member-Id", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 회원입니다."));
    }

    @Test
    @DisplayName("숫자가 아닌 X-Member-Id 헤더는 400을 응답한다")
    void invalidMemberIdHeader() throws Exception {
        mockMvc.perform(get("/api/books").header("X-Member-Id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("X-Member-Id 헤더가 올바르지 않습니다."));
    }
}
