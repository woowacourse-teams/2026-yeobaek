package yeobaek.backend.admin.dto;

import java.util.List;

public record MonitoringOverviewResponse(String environment, List<MonitoringRunResponse> measurements, List<MonitoringArtifactResponse> results) {
}
