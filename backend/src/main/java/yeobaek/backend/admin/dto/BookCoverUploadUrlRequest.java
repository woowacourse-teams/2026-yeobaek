package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record BookCoverUploadUrlRequest(
        @Schema(description = "이미지 MIME 타입", allowableValues = {"image/jpeg", "image/png", "image/webp"})
        @NotNull String contentType,
        @Schema(description = "이미지 크기(바이트), 최대 5 MiB") @NotNull Long contentLength
) {
}
