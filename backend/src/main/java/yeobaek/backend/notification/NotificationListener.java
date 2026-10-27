package yeobaek.backend.notification;

import static yeobaek.backend.support.LogField.CLUB_ID;
import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import yeobaek.backend.club.event.ClubMemberJoinedEvent;
import yeobaek.backend.comment.event.ClubSentenceCommentCreatedEvent;
import yeobaek.backend.comment.event.PublicRoomSentenceCommentCreatedEvent;
import yeobaek.backend.notification.clubjoin.ClubJoinNotificationService;
import yeobaek.backend.notification.comment.CommentNotificationService;
import yeobaek.backend.notification.domain.NotificationType;

@Component
@Slf4j
public class NotificationListener {
    private final CommentNotificationService commentNotifications;
    private final ClubJoinNotificationService clubJoinNotifications;
    private final TaskExecutor executor;

    public NotificationListener(CommentNotificationService commentNotifications,
                                ClubJoinNotificationService clubJoinNotifications,
                                @Qualifier("notificationTaskExecutor") TaskExecutor executor) {
        this.commentNotifications = commentNotifications;
        this.clubJoinNotifications = clubJoinNotifications;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClubSentenceCommentCreated(ClubSentenceCommentCreatedEvent event) {
        submit(NotificationType.CLUB_COMMENT, event.commentId(), () -> commentNotifications.notifyClub(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublicRoomSentenceCommentCreated(PublicRoomSentenceCommentCreatedEvent event) {
        submit(NotificationType.PUBLIC_ROOM_COMMENT, event.commentId(), () -> commentNotifications.notifyPublicRoom(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClubMemberJoined(ClubMemberJoinedEvent event) {
        submit(NotificationType.CLUB_JOIN, event.clubId(), () -> clubJoinNotifications.notifyMembers(event));
    }

    private void submit(NotificationType type, Long sourceId, Runnable task) {
        sourceLog(log.atInfo(), type, sourceId, "submit").log("알림 작업을 제출합니다.");
        try {
            CompletableFuture.runAsync(task, executor).whenComplete((ignored, exception) -> {
                if (exception != null) {
                    sourceLog(log.atError(), type, sourceId, "logUnexpectedFailure")
                            .addKeyValue(RESULT, "failure").setCause(exception)
                            .log("알림 워커에서 예상하지 못한 오류가 발생했습니다.");
                }
            });
        } catch (TaskRejectedException exception) {
            sourceLog(log.atWarn(), type, sourceId, "submit").addKeyValue(RESULT, "rejected")
                    .addKeyValue("reason", "executor_rejected")
                    .log("알림 작업을 제출하지 못했습니다. 대기열 포화 또는 실행기 종료 상태를 확인하세요.");
        }
    }

    private LoggingEventBuilder sourceLog(LoggingEventBuilder event, NotificationType type,
                                          Long sourceId, String action) {
        String prefix = type == NotificationType.CLUB_JOIN ? "notification.clubJoin." : "notification.comment.";
        return event.addKeyValue(OPERATION, prefix + action).addKeyValue("notificationType", type.name())
                .addKeyValue(type == NotificationType.CLUB_JOIN ? CLUB_ID : COMMENT_ID, sourceId);
    }
}
