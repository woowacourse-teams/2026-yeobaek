package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 인제스트 규격 JSON (API.md 6장, 2026-08-06 확정). 본문 순서는 배열 등장 순서로 서버가 부여한다.
 */
public record BookUploadRequest(
        @Schema(description = "도서 제목 (1~100자)") String title,
        @Schema(description = "출판사 (선택, 최대 100자)", nullable = true) String publisher,
        @Schema(description = "출판연도 (선택, 정수)", nullable = true) Integer publishedYear,
        @Schema(description = "작가 목록") List<AuthorEntryRequest> authors,
        @Schema(description = "목차 목록") List<ChapterUploadRequest> chapters
) {

    public BookUploadRequest {
        authors = authors == null ? List.of() : List.copyOf(authors);
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
    }
}
