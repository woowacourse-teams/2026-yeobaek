package yeobaek.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import yeobaek.backend.book.domain.vo.AuthorName;
import yeobaek.backend.book.domain.vo.Isni;

/**
 * 업로드 도서의 작가 항목. {name, isni?} 또는 {authorId} 중 한 형태만 허용한다 (API.md 6장).
 */
public record AuthorEntryRequest(
        @Schema(description = "기존 작가 참조 (선택)", nullable = true) Long authorId,
        @Schema(type = "string", description = "작가 이름 (신규·ISNI 형태)", nullable = true) AuthorName name,
        @Schema(type = "string", description = "ISNI (선택)", nullable = true) Isni isni
) {

    public AuthorEntryRequest {
        if (authorId == null && name == null) {
            throw new IllegalArgumentException("신규 작가 이름은 필수입니다.");
        }
    }

    public boolean referencesExisting() {
        return authorId != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue
    public boolean isNameProvidedWhenRequired() {
        return authorId != null || name != null;
    }
}
