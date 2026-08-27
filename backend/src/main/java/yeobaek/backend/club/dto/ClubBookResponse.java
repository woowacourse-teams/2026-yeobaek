package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.BookStatus;

public record ClubBookResponse(
        @Schema(description = "도서 ID") Long bookId,
        @Schema(description = "도서 제목") String title,
        @Schema(description = "작가 이름 목록") List<String> authors,
        @Schema(description = "표지 이미지 공개 URL", nullable = true) String coverImageUrl,
        @Schema(description = "본문 개수") int passageCount,
        @Schema(description = "도서 상태", allowableValues = {"ACTIVE", "DELETED"}) BookStatus status
) {

    public ClubBookResponse {
        authors = List.copyOf(authors);
    }

    public static ClubBookResponse of(Book book, List<String> authors, String coverImageUrl) {
        return new ClubBookResponse(book.getId(), book.getTitle(), authors, coverImageUrl,
                book.getPassageCount(), book.getStatus());
    }
}
