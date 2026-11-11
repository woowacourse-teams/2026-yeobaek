package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MonitoringStartResponse(
        @Schema(description = "생성했거나 재사용한 측정 UUID. 이 값만으로 측정 시작을 판단하지 않음")
        String session
) {
}
