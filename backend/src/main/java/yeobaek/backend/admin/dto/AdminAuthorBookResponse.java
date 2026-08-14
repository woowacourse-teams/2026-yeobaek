package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import yeobaek.backend.book.domain.Book;

public record AdminAuthorBookResponse(
        @Schema(description = "작품 도서 ID") Long bookId,
        @Schema(description = "작품 제목") String title
) {

    public static AdminAuthorBookResponse of(Book book) {
        return new AdminAuthorBookResponse(book.getId(), book.getTitle());
    }
}
