package yeobaek.backend.notification.comment;

import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import yeobaek.backend.comment.domain.ClubCommentCreated;
import yeobaek.backend.comment.domain.PublicRoomCommentCreated;

@Component
@Slf4j
public class CommentNotificationListener {

    private final CommentNotificationWorker worker;
    private final TaskExecutor executor;

    public CommentNotificationListener(CommentNotificationWorker worker,
                                       @Qualifier("notificationTaskExecutor") TaskExecutor executor) {
        this.worker = worker;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClubCommentCreated(ClubCommentCreated event) {
        submit(event.commentId(), () -> worker.notifyClub(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublicRoomCommentCreated(PublicRoomCommentCreated event) {
        submit(event.commentId(), () -> worker.notifyPublicRoom(event));
    }

    private void submit(Long commentId, Runnable task) {
        log.atInfo().addKeyValue(OPERATION, "notification.comment.submit").addKeyValue(COMMENT_ID, commentId)
                .log("댓글 알림 작업을 제출합니다.");
        try {
            CompletableFuture.runAsync(task, executor)
                    .whenComplete((ignored, exception) -> {
                        if (exception != null) {
                            log.atError().addKeyValue(OPERATION, "notification.comment.logUnexpectedFailure")
                                    .addKeyValue(RESULT, "failure").addKeyValue(COMMENT_ID, commentId)
                                    .setCause(exception).log("댓글 알림 워커에서 예상하지 못한 오류가 발생했습니다.");
                        }
                    });
        } catch (TaskRejectedException exception) {
            log.atWarn().addKeyValue(OPERATION, "notification.comment.submit")
                    .addKeyValue(RESULT, "rejected").addKeyValue(COMMENT_ID, commentId)
                    .addKeyValue("reason", "executor_rejected")
                    .log("알림 작업을 제출하지 못했습니다. 대기열 포화 또는 실행기 종료 상태를 확인하세요.");
        }
    }
}
