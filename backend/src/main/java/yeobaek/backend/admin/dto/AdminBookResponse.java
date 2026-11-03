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
        @Schema(description = "도서 상태", allowableValues = {"ACTIVE", "DELETED"}) BookStatus status,
        @Schema(description = "책 소개", nullable = true) String description,
        @Schema(description = "장르 목록") List<String> genres,
        @Schema(description = "해시태그 목록") List<String> hashtags
) {

    public AdminBookResponse(Long bookId, String title, List<AdminBookAuthorResponse> authors, String publisher,
                             Integer publishedYear, int passageCount, String coverImageUrl, BookStatus status,
                             String description) {
        this(bookId, title, authors, publisher, publishedYear, passageCount, coverImageUrl, status, description,
                List.of(), List.of());
    }

    public AdminBookResponse {
        authors = List.copyOf(authors);
        genres = List.copyOf(genres);
        hashtags = List.copyOf(hashtags);
    }

    public static AdminBookResponse of(
            Book book,
            List<AdminBookAuthorResponse> authors,
            String coverImageUrl
    ) {
        return new AdminBookResponse(
                book.getId(),
                book.getTitle().value(),
                authors,
                book.getPublisher() == null ? null : book.getPublisher().value(),
                book.getPublishedYear(),
                book.getPassageCount().value(),
                coverImageUrl,
                book.getStatus(),
                book.getDescription() == null ? null : book.getDescription().value(),
                book.genreValues(), book.hashtagValues());
    }
}
