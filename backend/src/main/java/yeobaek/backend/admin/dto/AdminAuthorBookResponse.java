package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;

public record AdminAuthorBookResponse(
        @Schema(description = "작품 도서 ID") Long bookId,
        @Schema(description = "작품 제목") String title,
        @Schema(description = "도서 상태", allowableValues = {"ACTIVE", "DELETED"}) BookStatus status
) {

    public static AdminAuthorBookResponse of(Book book) {
        return new AdminAuthorBookResponse(book.getId(), book.getTitle(), book.getStatus());
    }
}
