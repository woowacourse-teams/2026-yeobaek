package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;

public record AdminBookResponse(
        @Schema(description = "도서 ID") Long bookId,
        @Schema(description = "제목") String title,
        @Schema(description = "작가 목록") List<AdminBookAuthorResponse> authors,
        @Schema(description = "출판사", nullable = true) String publisher,
        @Schema(description = "출판 연도", nullable = true) Integer publishedYear,
        @Schema(description = "본문 수") int passageCount,
        @Schema(description = "표지 이미지 공개 URL", nullable = true) String coverImageUrl,
        @Schema(description = "도서 상태", allowableValues = {"ACTIVE", "DELETED"}) BookStatus status
) {

    public AdminBookResponse {
        authors = List.copyOf(authors);
    }

    public static AdminBookResponse of(
            Book book,
            List<AdminBookAuthorResponse> authors,
            String coverImageUrl
    ) {
        return new AdminBookResponse(
                book.getId(),
                book.getTitle(),
                authors,
                book.getPublisher(),
                book.getPublishedYear(),
                book.getPassageCount(),
                coverImageUrl,
                book.getStatus());
    }
}
