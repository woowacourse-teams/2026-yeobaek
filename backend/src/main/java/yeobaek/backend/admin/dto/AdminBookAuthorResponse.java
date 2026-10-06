package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminBookAuthorResponse(
        @Schema(description = "작가 ID") Long authorId,
        @Schema(description = "이름") String name,
        @Schema(description = "정규화된 ISNI (없으면 null)", nullable = true) String isni
) {
}
