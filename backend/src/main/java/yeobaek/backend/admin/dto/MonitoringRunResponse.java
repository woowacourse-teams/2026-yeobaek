package yeobaek.backend.admin.dto;


public record MonitoringRunResponse(long id, int attempt, String session, String title, String status,
                      String conclusion, String sha, String createdAt, String url, String phase) {
}
