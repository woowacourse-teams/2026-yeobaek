package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record BookCoverUpdateRequest(
        @Schema(description = "새 표지 UUID (기존 전체 객체 키도 허용)") @NotNull String coverImageKey
) {
}
