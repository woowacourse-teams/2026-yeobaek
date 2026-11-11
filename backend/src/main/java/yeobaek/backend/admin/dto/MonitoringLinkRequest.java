package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MonitoringLinkRequest(
        @Schema(description = "배포 목록에서 선택한 실행 id")
        @NotNull @Min(1) Long runId,
        @Schema(description = "선택한 실행의 attempt. 재실행을 구분하며 1부터 시작")
        @NotNull @Min(1) Integer attempt
) {
}
