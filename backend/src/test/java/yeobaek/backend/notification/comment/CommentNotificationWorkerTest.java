package yeobaek.backend.notification.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import yeobaek.backend.comment.domain.ClubCommentCreated;
import yeobaek.backend.comment.domain.PublicRoomCommentCreated;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.support.LogCapture;

class CommentNotificationWorkerTest {

    private final CommentNotificationService service = mock(CommentNotificationService.class);
    private final FcmFailureHandler handler = mock(FcmFailureHandler.class);
    private final CommentNotificationWorker worker = new CommentNotificationWorker(service, handler);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("모임·공개방 워커는 댓글 ID를 전달하고 반환된 재시도 후보 수만 기록한다")
    void recordCreatedTaskCount(boolean club) throws FirebaseMessagingException {
        var failure = new FcmSendFailure("c" + "a".repeat(21), "INTERNAL", "INTERNAL", 500, null);
        var task = new FcmRetryTask(11L, List.of(1L), "민감한 제목", "민감한 내용", failure);
        if (club) {
            given(service.notifyReadersInClub(11L, 5L, 13L, 7L)).willReturn(List.of(task));
        } else {
            given(service.notifyPreviousCommentersInPublicRoom(11L, 8L, 13L, 7L)).willReturn(List.of(task));
        }

        try (var logs = new LogCapture(CommentNotificationWorker.class.getName())) {
            notify(club);

            var event = logs.event("notification.comment.logRetryTasks", "success");
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(logs.field(event, "commentId")).isEqualTo(11L);
            assertThat(logs.field(event, "retryTaskCount")).isEqualTo(1);
            assertThat(logs.structuredText()).doesNotContain(failure.fid(), task.title(), task.body());
        }
        verifyNoInteractions(handler);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("SDK 호출 전체가 실패하면 작업 수 대신 기존 호출 실패 처리를 실행한다")
    void handleWholeCallFailure(boolean club) throws FirebaseMessagingException {
        var exception = mock(FirebaseMessagingException.class);
        if (club) {
            given(service.notifyReadersInClub(11L, 5L, 13L, 7L)).willThrow(exception);
        } else {
            given(service.notifyPreviousCommentersInPublicRoom(11L, 8L, 13L, 7L)).willThrow(exception);
        }

        try (var logs = new LogCapture(CommentNotificationWorker.class.getName())) {
            notify(club);

            assertThat(logs.structuredText()).doesNotContain("retryTaskCount");
        }
        verify(handler).handleCallFailure(11L, exception);
    }

    private void notify(boolean club) {
        if (club) {
            worker.notifyClub(new ClubCommentCreated(11L, 5L, 13L, 7L));
        } else {
            worker.notifyPublicRoom(new PublicRoomCommentCreated(11L, 8L, 13L, 7L));
        }
    }
}
