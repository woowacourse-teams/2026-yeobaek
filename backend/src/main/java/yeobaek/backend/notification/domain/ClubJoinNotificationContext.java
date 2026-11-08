package yeobaek.backend.notification.domain;

import java.time.Instant;
import java.util.Objects;

public record ClubJoinNotificationContext(Long clubId, Instant createdAt) implements NotificationContext {
    public ClubJoinNotificationContext {
        Objects.requireNonNull(clubId);
        Objects.requireNonNull(createdAt);
    }

    @Override
    public NotificationType notificationType() {
        return NotificationType.CLUB_JOIN;
    }

    @Override
    public Long sourceId() {
        return clubId;
    }
}
