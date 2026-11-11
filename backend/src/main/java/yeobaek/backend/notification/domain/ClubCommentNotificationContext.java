package yeobaek.backend.notification.domain;

import java.time.Instant;
import java.util.Objects;

public record ClubCommentNotificationContext(Long commentId, Long clubId, Instant createdAt) implements NotificationContext {
    public ClubCommentNotificationContext {
        Objects.requireNonNull(commentId);
        Objects.requireNonNull(clubId);
        Objects.requireNonNull(createdAt);
    }

    @Override
    public NotificationType notificationType() {
        return NotificationType.CLUB_COMMENT;
    }

    @Override
    public Long sourceId() {
        return commentId;
    }
}
