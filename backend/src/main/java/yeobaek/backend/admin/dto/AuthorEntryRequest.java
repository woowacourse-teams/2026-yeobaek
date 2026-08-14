package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 업로드 도서의 작가 항목. {name, isni?} 또는 {authorId} 중 한 형태만 허용한다 (API.md 6장).
 */
public record AuthorEntryRequest(
        @Schema(description = "기존 작가 참조 (선택)", nullable = true) Long authorId,
        @Schema(description = "작가 이름 (신규·ISNI 형태)", nullable = true) String name,
        @Schema(description = "ISNI (선택)", nullable = true) String isni
) {

    public boolean referencesExisting() {
        return authorId != null;
    }
}
