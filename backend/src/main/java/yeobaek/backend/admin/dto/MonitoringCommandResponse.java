package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MonitoringCommandResponse(
        @Schema(description = "요청 종류: stop 또는 link")
        String command,
        @Schema(description = "명령 전달 워크플로 상태")
        String status,
        @Schema(description = "명령 전달 결과. 종료 전에는 null")
        String conclusion,
        @Schema(description = "GitHub 명령 실행 상세 주소")
        String url
) {
}
