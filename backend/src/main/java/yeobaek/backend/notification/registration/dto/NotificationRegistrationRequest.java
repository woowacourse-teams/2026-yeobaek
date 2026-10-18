package yeobaek.backend.notification.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationRegistrationRequest(
        @Schema(description = "FID") String fid
) {
}
