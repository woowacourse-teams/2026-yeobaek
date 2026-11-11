package yeobaek.backend.notification.registration.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationClubPreferenceUpdateRequest(@NotNull Boolean enabled) {
}
