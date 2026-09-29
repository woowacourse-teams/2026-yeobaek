package yeobaek.backend.book.controller;

import static yeobaek.backend.support.LogField.BOOK_ID;
import static yeobaek.backend.support.LogField.MEMBER_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.SUCCESS;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.auth.AuthMember;
import yeobaek.backend.book.dto.BookDetailResponse;
import yeobaek.backend.book.dto.BooksResponse;
import yeobaek.backend.book.service.BookService;
import yeobaek.backend.support.analytics.AnalyticsEvent;
import yeobaek.backend.support.analytics.AnalyticsTracker;

@Tag(name = "도서")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookService bookService;
    private final AnalyticsTracker analyticsTracker;

    @Operation(summary = "도서 목록 조회 · 검색",
            description = "모임 생성 시 선택할 도서 목록을 조회한다. keyword를 주면 제목 또는 작가 이름 부분 일치로 검색한다.")
    @GetMapping("/api/books")
    public BooksResponse findBooks(
            @AuthMember Long memberId,
            @Parameter(description = "제목 또는 작가 이름 부분 일치 검색어. 미지정·공백이면 전체 목록")
            @RequestParam(required = false) String keyword) {
        log.atInfo().addKeyValue(OPERATION, "book.findBooks").addKeyValue(MEMBER_ID, memberId)
                .addKeyValue("keyword", keyword)
                .addKeyValue("searchUsed", keyword != null && !keyword.isBlank()).log("도서 목록 API 처리를 시작합니다.");
        BooksResponse response = bookService.findBooks(keyword);
        analyticsTracker.track(memberId,
                AnalyticsEvent.booksView(keyword != null && !keyword.isBlank(), response.books().size()));
        log.atInfo().addKeyValue(OPERATION, "book.findBooks").addKeyValue(PHASE, SUCCESS).addKeyValue(MEMBER_ID, memberId)
                .addKeyValue("keyword", keyword)
                .addKeyValue("searchUsed", keyword != null && !keyword.isBlank())
                .addKeyValue("resultCount", response.books().size()).log("도서 목록 API 처리를 완료했습니다.");
        return response;
    }

    @Operation(summary = "도서 상세 + 목차 조회")
    @GetMapping("/api/books/{bookId}")
    public BookDetailResponse findBook(@AuthMember Long memberId,
                                       @Parameter(description = "도서 ID") @PathVariable Long bookId) {
        log.atInfo().addKeyValue(OPERATION, "book.findBook").addKeyValue(MEMBER_ID, memberId)
                .addKeyValue(BOOK_ID, bookId).log("도서 상세 API 처리를 시작합니다.");
        BookDetailResponse response = bookService.findBook(bookId);
        analyticsTracker.track(memberId,
                AnalyticsEvent.bookView(response.bookId(), response.passageCount(), response.chapters().size()));
        log.atInfo().addKeyValue(OPERATION, "book.findBook").addKeyValue(PHASE, SUCCESS).addKeyValue(MEMBER_ID, memberId)
                .addKeyValue(BOOK_ID, bookId).log("도서 상세 API 처리를 완료했습니다.");
        return response;
    }
}
