package yeobaek.backend.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

public record MonitoringSessionResponse(
        @Schema(description = "측정 워크플로 실행")
        MonitoringRunResponse run,
        @Schema(description = "요청 시작 확인 전에는 null. 확인 후 session, environment, target, startedAt, maxDurationSeconds, intervalSeconds, requestTimeoutSeconds", type = "object", implementation = Map.class)
        JsonNode state,
        @Schema(description = "연결이 반영된 배포·롤백")
        List<MonitoringRunResponse> deployments,
        @Schema(description = "종료·연결 명령 전달 상태")
        List<MonitoringCommandResponse> commands,
        @Schema(description = "보관 결과 목록에서 조회할 수 있는 결과 존재 여부")
        boolean resultAvailable,
        @Schema(description = "서버가 상태를 조회한 시각 (UTC)")
        String checkedAt,
        @Schema(description = "연결한 모든 배포 종료 후 5분 시각. 배포 없음·진행 중·종료 시각 불명은 null. 전체 30분 제한은 별도 적용")
        String observationEndsAt
) {
}
