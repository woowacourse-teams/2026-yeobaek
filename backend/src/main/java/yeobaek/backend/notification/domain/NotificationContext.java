package yeobaek.backend.notification.domain;

import java.time.Instant;

public sealed interface NotificationContext permits ClubJoinNotificationContext,
        ClubCommentNotificationContext, PublicRoomCommentNotificationContext {
    NotificationType notificationType();

    Long sourceId();

    Instant createdAt();
}
