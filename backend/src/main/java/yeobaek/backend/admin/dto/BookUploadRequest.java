package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 인제스트 규격 JSON (API.md 6장, 2026-08-06 확정). 본문 순서는 배열 등장 순서로 서버가 부여한다.
 */
public record BookUploadRequest(
        @Schema(description = "도서 제목 (1~100자)") @NotNull String title,
        @Schema(description = "출판사 (선택, 최대 100자)", nullable = true) String publisher,
        @Schema(description = "출판연도 (선택, 정수)", nullable = true) Integer publishedYear,
        @Schema(description = "표지 이미지 객체 키 (선택)", nullable = true) String coverImageKey,
        @Schema(description = "작가 목록") @Valid @NotNull List<@NotNull AuthorEntryRequest> authors,
        @Schema(description = "목차 목록") @Valid @NotNull List<@NotNull ChapterUploadRequest> chapters
) {

    public BookUploadRequest {
        if (authors != null) {
            authors = Collections.unmodifiableList(new ArrayList<>(authors));
        }
        if (chapters != null) {
            chapters = Collections.unmodifiableList(new ArrayList<>(chapters));
        }
    }

    @Override
    public List<ChapterUploadRequest> chapters() {
        return chapters == null ? null : Collections.unmodifiableList(chapters);
    }

    @Override
    public List<AuthorEntryRequest> authors() {
        return authors == null ? null : Collections.unmodifiableList(authors);
    }
}
