package yeobaek.backend.admin.dto;


public record MonitoringArtifactResponse(long id, String session, String createdAt, String expiresAt, long bytes) {
}
