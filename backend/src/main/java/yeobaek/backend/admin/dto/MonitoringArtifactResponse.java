package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MonitoringArtifactResponse(
        @Schema(description = "결과 조회에 사용하는 아티팩트 ID")
        long id,
        @Schema(description = "측정 UUID")
        String session,
        @Schema(description = "결과 저장 시각 (UTC)")
        String createdAt,
        @Schema(description = "결과 만료 시각 (UTC)")
        String expiresAt,
        @Schema(description = "압축된 결과 파일 크기 (바이트)")
        long bytes
) {
}
