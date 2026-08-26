package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

public record BookCoverUploadUrlResponse(
        @Schema(description = "도서에 저장할 S3 객체 키") String coverImageKey,
        @Schema(description = "S3 Presigned PUT URL") String uploadUrl,
        @Schema(description = "업로드 URL 만료 시각") Instant expiresAt,
        @Schema(description = "S3 PUT 시 반드시 전송할 헤더") Map<String, String> requiredHeaders
) {
    public BookCoverUploadUrlResponse {
        requiredHeaders = Map.copyOf(requiredHeaders);
    }
}
