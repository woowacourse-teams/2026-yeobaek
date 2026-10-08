package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BookCoverUpdateRequest(
        @Schema(description = "새 표지 UUID (소문자, 경로·확장자 없음)")
        @NotNull
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                message = "표지 키는 경로와 확장자가 없는 소문자 UUID여야 합니다.")
        String coverImageKey
) {
}
