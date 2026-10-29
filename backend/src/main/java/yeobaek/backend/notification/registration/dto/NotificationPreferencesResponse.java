package yeobaek.backend.notification.registration.dto;

import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.domain.NotificationPreferences;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;

public record NotificationPreferencesResponse(
        boolean enabled,
        boolean clubJoin,
        boolean clubComment,
        boolean publicRoomComment
) {
    public static NotificationPreferencesResponse from(
            NotificationRegistration registration, NotificationPreferences preferences) {
        return new NotificationPreferencesResponse(registration.isEnabled(),
                preferences.isTypeEnabled(NotificationType.CLUB_JOIN),
                preferences.isTypeEnabled(NotificationType.CLUB_COMMENT),
                preferences.isTypeEnabled(NotificationType.PUBLIC_ROOM_COMMENT));
    }
}
