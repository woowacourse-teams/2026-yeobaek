package watson.backend.admin.dto;

import java.util.List;
import watson.backend.book.domain.Author;

public record AdminAuthorResponse(
        Long authorId,
        String name,
        String isni,
        List<AdminAuthorBookResponse> books
) {

    public AdminAuthorResponse {
        books = List.copyOf(books);
    }

    public static AdminAuthorResponse of(Author author, List<AdminAuthorBookResponse> books) {
        return new AdminAuthorResponse(author.getId(), author.getName(), author.getIsni(), books);
    }
}
