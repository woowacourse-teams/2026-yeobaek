package watson.backend.book.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import watson.backend.book.dto.BookDetailResponse;
import watson.backend.book.dto.BooksResponse;
import watson.backend.book.service.BookService;

@RestController
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/api/books")
    public BooksResponse findBooks() {
        return bookService.findBooks();
    }

    @GetMapping("/api/books/{bookId}")
    public BookDetailResponse findBook(@PathVariable Long bookId) {
        return bookService.findBook(bookId);
    }
}
