package yeobaek.backend.notification.comment;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

class CommentNotificationServiceTest {

    private final CommentNotificationRecipientReader reader = mock(CommentNotificationRecipientReader.class);
    private final FcmPushSender sender = mock(FcmPushSender.class);
    private final NotificationRegistrationService registrationService = mock(NotificationRegistrationService.class);
    private final FcmFailureHandler handler = mock(FcmFailureHandler.class);
    private final CommentNotificationService service = new CommentNotificationService(
            reader, sender, registrationService, handler);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("모임·공개방 모두 발송 전에 등록을 확인하고 SDK 결과를 실패 처리에 넘긴다")
    void handleResultAfterSnapshotAndSend(boolean club) throws FirebaseMessagingException {
        List<String> fids = List.of("c" + "a".repeat(21));
        var snapshot = new NotificationRegistrations(List.of());
        var result = new FcmSendResult(1, List.of());
        stubRecipients(club, fids);
        given(registrationService.snapshotForSend(fids)).willReturn(snapshot);
        given(sender.send(fids, "새로운 댓글이 달렸어요!", "댓글을 남겼던 문장의 이야기를 확인해 보세요."))
                .willReturn(result);

        notify(club);

        var order = inOrder(registrationService, sender, handler);
        order.verify(registrationService).snapshotForSend(fids);
        order.verify(sender).send(fids, "새로운 댓글이 달렸어요!", "댓글을 남겼던 문장의 이야기를 확인해 보세요.");
        order.verify(handler).handle(result, snapshot);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("수신 대상이 없으면 스냅샷 조회·발송·실패 처리를 하지 않는다")
    void skipEmptyRecipients(boolean club) throws FirebaseMessagingException {
        stubRecipients(club, List.of());

        notify(club);

        verifyNoInteractions(registrationService, sender, handler);
    }

    private void stubRecipients(boolean club, List<String> fids) {
        if (club) {
            given(reader.findReadersInClub(5L, 13L, 7L)).willReturn(fids);
        } else {
            given(reader.findPreviousCommentersInPublicRoom(8L, 13L, 7L)).willReturn(fids);
        }
    }

    private void notify(boolean club) throws FirebaseMessagingException {
        if (club) {
            service.notifyReadersInClub(5L, 13L, 7L);
        } else {
            service.notifyPreviousCommentersInPublicRoom(8L, 13L, 7L);
        }
    }
}
