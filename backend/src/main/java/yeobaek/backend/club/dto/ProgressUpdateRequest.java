package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProgressUpdateRequest(
        @Schema(description = "열람한 본문 ID") Long passageId
) {
}
