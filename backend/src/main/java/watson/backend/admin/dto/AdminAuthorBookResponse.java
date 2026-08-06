package watson.backend.admin.dto;

import watson.backend.book.domain.Book;

public record AdminAuthorBookResponse(
        Long bookId,
        String title
) {

    public static AdminAuthorBookResponse of(Book book) {
        return new AdminAuthorBookResponse(book.getId(), book.getTitle());
    }
}
