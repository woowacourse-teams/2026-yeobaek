package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PassageUploadRequest(
        @Schema(description = "본문 내용 (필수)") String content
) {
}
