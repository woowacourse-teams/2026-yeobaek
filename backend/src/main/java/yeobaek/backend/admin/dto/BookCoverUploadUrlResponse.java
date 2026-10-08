package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

public record BookCoverUploadUrlResponse(
        @Schema(description = "도서에 저장할 표지 UUID") String coverImageKey,
        @Schema(description = "orgin/{uuid} 원본 업로드") UploadTarget original,
        @Schema(description = "low/{uuid} JPEG 압축본 업로드") UploadTarget low,
        @Schema(description = "{uuid} 구버전용 JPEG 압축본 업로드") UploadTarget legacy
) {
    public record UploadTarget(
            @Schema(description = "S3 Presigned PUT URL") String uploadUrl,
            @Schema(description = "업로드 URL 만료 시각") Instant expiresAt,
            @Schema(description = "S3 PUT 시 반드시 전송할 헤더") Map<String, String> requiredHeaders
    ) {
        public UploadTarget {
            requiredHeaders = Map.copyOf(requiredHeaders);
        }
    }
}
