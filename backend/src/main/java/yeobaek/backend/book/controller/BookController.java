package yeobaek.backend.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yeobaek.backend.book.dto.BookDetailResponse;
import yeobaek.backend.book.dto.BooksResponse;
import yeobaek.backend.book.service.BookService;

@Tag(name = "도서")
@SecurityRequirement(name = "memberId")
@RestController
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "도서 목록 조회 · 검색",
            description = "모임 생성 시 선택할 도서 목록을 조회한다. keyword를 주면 제목 또는 작가 이름 부분 일치로 검색한다.")
    @GetMapping("/api/books")
    public BooksResponse findBooks(
            @Parameter(description = "제목 또는 작가 이름 부분 일치 검색어. 미지정·공백이면 전체 목록")
            @RequestParam(required = false) String keyword) {
        return bookService.findBooks(keyword);
    }

    @Operation(summary = "도서 상세 + 목차 조회")
    @GetMapping("/api/books/{bookId}")
    public BookDetailResponse findBook(@Parameter(description = "도서 ID") @PathVariable Long bookId) {
        return bookService.findBook(bookId);
    }
}
