package yeobaek.backend.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import yeobaek.backend.book.domain.Book;

public record BookDetailResponse(
        @Schema(description = "도서 ID") Long bookId,
        @Schema(description = "제목") String title,
        @Schema(description = "작가 이름 목록") List<String> authors,
        @Schema(description = "출판사", nullable = true) String publisher,
        @Schema(description = "출판연도", nullable = true) Integer publishedYear,
        @Schema(description = "표지 이미지 공개 URL", nullable = true) String coverImageUrl,
        @Schema(description = "본문 개수") int passageCount,
        @Schema(description = "목차 목록") List<ChapterResponse> chapters,
        @Schema(description = "책 소개", nullable = true) String description,
        @Schema(description = "본문 700자 기준 환산 쪽수. 기존 도서 보완 전에는 null", nullable = true) Long pageCount,
        @Schema(description = "장르 목록") List<String> genres,
        @Schema(description = "해시태그 목록") List<String> hashtags
) {

    public BookDetailResponse(Long bookId, String title, List<String> authors, String publisher,
                              Integer publishedYear, String coverImageUrl, int passageCount,
                              List<ChapterResponse> chapters, String description, Long pageCount) {
        this(bookId, title, authors, publisher, publishedYear, coverImageUrl, passageCount, chapters,
                description, pageCount, List.of(), List.of());
    }

    public BookDetailResponse {
        authors = List.copyOf(authors);
        chapters = List.copyOf(chapters);
        genres = List.copyOf(genres);
        hashtags = List.copyOf(hashtags);
    }

    public static BookDetailResponse of(Book book, List<String> authors, String coverImageUrl,
                                        List<ChapterResponse> chapters) {
        return new BookDetailResponse(book.getId(), book.getTitle().value(), authors,
                book.getPublisher() == null ? null : book.getPublisher().value(), book.getPublishedYear(),
                coverImageUrl, book.getPassageCount().value(), chapters,
                book.getDescription() == null ? null : book.getDescription().value(), book.pageCount(),
                book.genreValues(), book.hashtagValues());
    }
}
