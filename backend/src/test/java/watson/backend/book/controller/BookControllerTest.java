package watson.backend.book.controller;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import watson.backend.book.dto.BookDetailResponse;
import watson.backend.book.dto.BookSummaryResponse;
import watson.backend.book.dto.BooksResponse;
import watson.backend.book.dto.ChapterResponse;
import watson.backend.book.service.BookService;
import watson.backend.member.domain.Member;
import watson.backend.support.ControllerTest;
import watson.backend.support.NotFoundException;

@WebMvcTest(BookController.class)
class BookControllerTest extends ControllerTest {

    @MockitoBean
    private BookService bookService;

    @Test
    @DisplayName("도서 목록을 조회한다")
    void findBooks() throws Exception {
        givenValidMember(1L);
        given(bookService.findBooks()).willReturn(new BooksResponse(List.of(
                new BookSummaryResponse(1L, "운수 좋은 날", List.of("현진건"), "자체 제작", 1924, 312))));

        mockMvc.perform(get("/api/books").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].bookId").value(1))
                .andExpect(jsonPath("$.books[0].authors[0]").value("현진건"))
                .andDo(document("book-list", resource(ResourceSnippetParameters.builder()
                        .tag("도서")
                        .summary("도서 목록 조회")
                        .description("모임 생성 시 선택할 도서 목록을 조회한다.")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("books[].bookId").description("도서 ID"),
                                PayloadDocumentation.fieldWithPath("books[].title").description("제목"),
                                PayloadDocumentation.fieldWithPath("books[].authors[]").description("작가 이름 목록"),
                                PayloadDocumentation.fieldWithPath("books[].publisher").description("출판사").optional(),
                                PayloadDocumentation.fieldWithPath("books[].publishedYear").description("출판연도").optional(),
                                PayloadDocumentation.fieldWithPath("books[].passageCount").description("본문 개수"))
                        .build())));
    }

    @Test
    @DisplayName("도서 상세와 목차를 조회한다")
    void findBook() throws Exception {
        givenValidMember(1L);
        given(bookService.findBook(1L)).willReturn(new BookDetailResponse(1L, "운수 좋은 날", List.of("현진건"),
                "자체 제작", 1924, 312, List.of(new ChapterResponse(1L, "1장", 1, 1, 105))));

        mockMvc.perform(get("/api/books/1").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters[0].startPassageSequence").value(1))
                .andExpect(jsonPath("$.chapters[0].endPassageSequence").value(105))
                .andDo(document("book-detail", resource(ResourceSnippetParameters.builder()
                        .tag("도서")
                        .summary("도서 상세 + 목차 조회")
                        .requestHeaders(headerWithName("X-Member-Id").description("회원 ID"))
                        .responseFields(
                                PayloadDocumentation.fieldWithPath("bookId").description("도서 ID"),
                                PayloadDocumentation.fieldWithPath("title").description("제목"),
                                PayloadDocumentation.fieldWithPath("authors[]").description("작가 이름 목록"),
                                PayloadDocumentation.fieldWithPath("publisher").description("출판사").optional(),
                                PayloadDocumentation.fieldWithPath("publishedYear").description("출판연도").optional(),
                                PayloadDocumentation.fieldWithPath("passageCount").description("본문 개수"),
                                PayloadDocumentation.fieldWithPath("chapters[].chapterId").description("목차 ID"),
                                PayloadDocumentation.fieldWithPath("chapters[].title").description("목차 제목"),
                                PayloadDocumentation.fieldWithPath("chapters[].sequence").description("목차 순서"),
                                PayloadDocumentation.fieldWithPath("chapters[].startPassageSequence").description("챕터 시작 본문의 전체 순서"),
                                PayloadDocumentation.fieldWithPath("chapters[].endPassageSequence").description("챕터 끝 본문의 전체 순서"))
                        .build())));
    }

    @Test
    @DisplayName("존재하지 않는 도서는 404를 응답한다")
    void findBookNotFound() throws Exception {
        givenValidMember(1L);
        given(bookService.findBook(anyLong())).willThrow(new NotFoundException("존재하지 않는 도서입니다."));

        mockMvc.perform(get("/api/books/999").header("X-Member-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 도서입니다."));
    }

    @Test
    @DisplayName("X-Member-Id 헤더가 없으면 400을 응답한다")
    void missingMemberIdHeader() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("X-Member-Id 헤더가 필요합니다."));
    }

    @Test
    @DisplayName("존재하지 않는 회원의 요청은 400을 응답한다")
    void unknownMember() throws Exception {
        given(memberRepository.existsById(99L)).willReturn(false);

        mockMvc.perform(get("/api/books").header("X-Member-Id", "99"))
                .andExpect(status().isBadRequest())
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
