package yeobaek.backend.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import yeobaek.backend.book.domain.Book;

public record BookSummaryResponse(
        @Schema(description = "도서 ID") Long bookId,
        @Schema(description = "제목") String title,
        @Schema(description = "작가 이름 목록") List<String> authors,
        @Schema(description = "출판사", nullable = true) String publisher,
        @Schema(description = "출판연도", nullable = true) Integer publishedYear,
        @Schema(description = "표지 이미지 공개 URL", nullable = true) String coverImageUrl,
        @Schema(description = "본문 개수") int passageCount
) {

    public BookSummaryResponse {
        authors = List.copyOf(authors);
    }

    public static BookSummaryResponse of(Book book, List<String> authors, String coverImageUrl) {
        return new BookSummaryResponse(book.getId(), book.getTitle(), authors,
                book.getPublisher(), book.getPublishedYear(), coverImageUrl, book.getPassageCount());
    }
}
