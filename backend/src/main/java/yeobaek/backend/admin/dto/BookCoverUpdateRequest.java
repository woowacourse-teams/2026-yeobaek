package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BookCoverUpdateRequest(
        @Schema(description = "새 표지 이미지 객체 키") String coverImageKey
) {
}
