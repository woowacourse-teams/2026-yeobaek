package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SentenceUploadRequest(
        @Schema(description = "원문의 공백과 개행을 보존한 문장 내용 (필수)") String content
) {
}
