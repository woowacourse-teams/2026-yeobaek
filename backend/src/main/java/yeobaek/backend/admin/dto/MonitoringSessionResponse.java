package yeobaek.backend.admin.dto;

import java.util.List;
import tools.jackson.databind.JsonNode;

public record MonitoringSessionResponse(MonitoringRunResponse run, JsonNode state, List<MonitoringRunResponse> deployments, List<MonitoringCommandResponse> commands,
                          boolean resultAvailable, String checkedAt, String observationEndsAt) {
}
