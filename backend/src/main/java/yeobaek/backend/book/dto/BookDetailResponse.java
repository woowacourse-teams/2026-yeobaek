package yeobaek.backend.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BookDetailResponse(
        @Schema(description = "도서 ID") Long bookId,
        @Schema(description = "제목") String title,
        @Schema(description = "작가 이름 목록") List<String> authors,
        @Schema(description = "출판사", nullable = true) String publisher,
        @Schema(description = "출판연도", nullable = true) Integer publishedYear,
        @Schema(description = "표지 이미지 공개 URL", nullable = true) String coverImageUrl,
        @Schema(description = "본문 개수") int passageCount,
        @Schema(description = "목차 목록") List<ChapterResponse> chapters
) {

    public BookDetailResponse {
        authors = List.copyOf(authors);
        chapters = List.copyOf(chapters);
    }
}
