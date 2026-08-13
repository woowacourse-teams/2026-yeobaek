package yeobaek.backend.admin.dto;

import yeobaek.backend.book.domain.Book;

public record AdminAuthorBookResponse(
        Long bookId,
        String title
) {

    public static AdminAuthorBookResponse of(Book book) {
        return new AdminAuthorBookResponse(book.getId(), book.getTitle());
    }
}
