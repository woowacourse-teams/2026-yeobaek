package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MonitoringOverviewResponse(
        @Schema(description = "측정 대상 환경: dev 또는 prod")
        String environment,
        @Schema(description = "최근 측정 실행 최대 20개. 결과가 없는 실행도 포함")
        List<MonitoringRunResponse> measurements,
        @Schema(description = "보관 중인 결과 최대 20개. 기간 만료 시 더 적을 수 있음")
        List<MonitoringArtifactResponse> results
) {
}
