package yeobaek.backend.notification.domain;

import java.time.Instant;
import java.util.Objects;

public record PublicRoomCommentNotificationContext(Long commentId, Instant createdAt) implements NotificationContext {
    public PublicRoomCommentNotificationContext {
        Objects.requireNonNull(commentId);
        Objects.requireNonNull(createdAt);
    }

    @Override
    public NotificationType notificationType() {
        return NotificationType.PUBLIC_ROOM_COMMENT;
    }

    @Override
    public Long sourceId() {
        return commentId;
    }
}
