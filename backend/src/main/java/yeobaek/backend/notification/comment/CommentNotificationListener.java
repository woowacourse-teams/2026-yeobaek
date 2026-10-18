package yeobaek.backend.notification.comment;

import static yeobaek.backend.support.LogField.COMMENT_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;

import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import yeobaek.backend.comment.domain.ClubCommentCreated;
import yeobaek.backend.comment.domain.PublicRoomCommentCreated;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentNotificationListener {

    private final CommentNotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClubCommentCreated(ClubCommentCreated event) {
        try {
            notificationService.notifyReadersInClub(event.clubId(), event.sentenceId(), event.writerId());
        } catch (FirebaseMessagingException | DataAccessException exception) {
            logFailure(event.commentId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublicRoomCommentCreated(PublicRoomCommentCreated event) {
        try {
            notificationService.notifyPreviousCommentersInPublicRoom(
                    event.publicRoomId(), event.sentenceId(), event.writerId());
        } catch (FirebaseMessagingException | DataAccessException exception) {
            logFailure(event.commentId(), exception);
        }
    }

    private void logFailure(Long commentId, Exception exception) {
        log.atError().addKeyValue(OPERATION, "notification.comment.send")
                .addKeyValue(RESULT, "failure").addKeyValue(COMMENT_ID, commentId)
                .setCause(exception).log("댓글 저장 후 알림 처리에 실패했습니다.");
    }
}
