package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import yeobaek.backend.book.domain.Author;

public record AdminAuthorResponse(
        @Schema(description = "작가 ID") Long authorId,
        @Schema(description = "이름") String name,
        @Schema(description = "정규화된 ISNI (없으면 null)", nullable = true) String isni,
        @Schema(description = "작품 목록") List<AdminAuthorBookResponse> books
) {

    public AdminAuthorResponse {
        books = List.copyOf(books);
    }

    public static AdminAuthorResponse of(Author author, List<AdminAuthorBookResponse> books) {
        return new AdminAuthorResponse(author.getId(), author.getName(), author.getIsni(), books);
    }
}
