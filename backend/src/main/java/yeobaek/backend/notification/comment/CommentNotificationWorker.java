package yeobaek.backend.notification.comment;

import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import yeobaek.backend.comment.event.ClubSentenceCommentCreatedEvent;
import yeobaek.backend.comment.event.PublicRoomSentenceCommentCreatedEvent;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmRetryProcessor;
import yeobaek.backend.notification.domain.FcmRetryTask;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentNotificationWorker {

    private final CommentNotificationService notificationService;
    private final FcmFailureHandler failureHandler;
    private final FcmRetryProcessor retryProcessor;

    public void notifyClub(ClubSentenceCommentCreatedEvent event) {
        try {
            List<FcmRetryTask> tasks = notificationService.notifyReadersInClub(
                    event.commentId(), event.clubId(), event.sentenceId(), event.writerId(),
                    event.passageSequence(), event.commentCreatedAt());
            logRetryTasks(event.commentId(), tasks);
        } catch (FirebaseMessagingException exception) {
            failureHandler.handleCallFailure(event.commentId(), exception);
        } catch (IllegalArgumentException exception) {
            failureHandler.handleInvalidRequest(event.commentId());
        } catch (DataAccessException exception) {
            logFailure(event.commentId(), exception);
        }
    }

    public void notifyPublicRoom(PublicRoomSentenceCommentCreatedEvent event) {
        try {
            List<FcmRetryTask> tasks = notificationService.notifyPreviousCommentersInPublicRoom(
                    event.commentId(), event.publicRoomId(), event.sentenceId(), event.writerId(),
                    event.passageSequence(), event.commentCreatedAt());
            logRetryTasks(event.commentId(), tasks);
        } catch (FirebaseMessagingException exception) {
            failureHandler.handleCallFailure(event.commentId(), exception);
        } catch (IllegalArgumentException exception) {
            failureHandler.handleInvalidRequest(event.commentId());
        } catch (DataAccessException exception) {
            logFailure(event.commentId(), exception);
        }
    }

    private void logRetryTasks(Long commentId, List<FcmRetryTask> tasks) {
        tasks.forEach(retryProcessor::schedule);
        log.atInfo().addKeyValue(OPERATION, "notification.comment.logRetryTasks")
                .addKeyValue(RESULT, "success").addKeyValue(COMMENT_ID, commentId)
                .addKeyValue("retryTaskCount", tasks.size())
                .log("댓글 알림의 재시도 후보 작업 수를 확인했습니다.");
    }

    private void logFailure(Long commentId, DataAccessException exception) {
        log.atError().addKeyValue(OPERATION, "notification.comment.logFailure")
                .addKeyValue(RESULT, "failure").addKeyValue(COMMENT_ID, commentId)
                .setCause(exception).log("댓글 저장 후 알림 워커에서 처리에 실패했습니다.");
    }
}
