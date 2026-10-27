package yeobaek.backend.notification.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.EnumMap;
import java.util.Map;
import yeobaek.backend.notification.domain.NotificationType;

@Schema(description = "변경할 항목만 전달해요. 생략하거나 null인 항목은 기존 값을 유지해요.")
public record NotificationPreferencesUpdateRequest(
        Boolean enabled,
        Boolean clubJoin,
        Boolean clubComment,
        Boolean publicRoomComment
) {
    public Map<NotificationType, Boolean> typeChanges() {
        Map<NotificationType, Boolean> changes = new EnumMap<>(NotificationType.class);
        addIfPresent(changes, NotificationType.CLUB_JOIN, clubJoin);
        addIfPresent(changes, NotificationType.CLUB_COMMENT, clubComment);
        addIfPresent(changes, NotificationType.PUBLIC_ROOM_COMMENT, publicRoomComment);
        return changes;
    }

    private void addIfPresent(Map<NotificationType, Boolean> changes, NotificationType type, Boolean enabled) {
        if (enabled != null) {
            changes.put(type, enabled);
        }
    }
}
