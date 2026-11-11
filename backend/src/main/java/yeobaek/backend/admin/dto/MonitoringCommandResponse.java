package yeobaek.backend.admin.dto;


public record MonitoringCommandResponse(String command, String status, String conclusion, String url) {
}
