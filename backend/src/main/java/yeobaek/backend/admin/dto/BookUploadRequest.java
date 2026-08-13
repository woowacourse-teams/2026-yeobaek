package yeobaek.backend.admin.dto;

import java.util.List;

/**
 * 인제스트 규격 JSON (API.md 6장, 2026-08-06 확정). 본문 순서는 배열 등장 순서로 서버가 부여한다.
 */
public record BookUploadRequest(
        String title,
        String publisher,
        Integer publishedYear,
        List<AuthorEntryRequest> authors,
        List<ChapterUploadRequest> chapters
) {

    public BookUploadRequest {
        authors = authors == null ? List.of() : List.copyOf(authors);
        chapters = chapters == null ? List.of() : List.copyOf(chapters);
    }
}
