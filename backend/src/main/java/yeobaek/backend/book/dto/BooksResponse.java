package yeobaek.backend.book.dto;

import java.util.List;

public record BooksResponse(List<BookSummaryResponse> books) {

    public BooksResponse {
        books = List.copyOf(books);
    }
}
