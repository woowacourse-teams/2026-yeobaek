package yeobaek.backend.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ProgressUpdateRequest(
        @Schema(description = "열람한 본문 ID") @NotNull Long passageId
) {
}
