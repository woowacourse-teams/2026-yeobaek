package yeobaek.backend.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MonitoringLinkRequest(@NotNull @Min(1) Long runId, @NotNull @Min(1) Integer attempt) {
}
