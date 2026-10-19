package yeobaek.backend.notification.comment;

import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentNotificationService {
    private final CommentNotificationRecipientReader recipientReader;
    private final FcmPushSender pushSender;
    private final NotificationRegistrationService registrationService;
    private final FcmFailureHandler failureHandler;

    public void notifyReadersInClub(Long clubId, Long sentenceId, Long writerId) throws FirebaseMessagingException {
        List<String> firebaseInstallationIds = recipientReader.findReadersInClub(clubId, sentenceId, writerId);

        notifyRecipients(firebaseInstallationIds);
    }

    public void notifyPreviousCommentersInPublicRoom(Long publicRoomId, Long sentenceId, Long writerId)
            throws FirebaseMessagingException {
        List<String> firebaseInstallationIds = recipientReader.findPreviousCommentersInPublicRoom(
                publicRoomId, sentenceId, writerId);

        notifyRecipients(firebaseInstallationIds);
    }

    private void notifyRecipients(List<String> firebaseInstallationIds) throws FirebaseMessagingException {
        if (firebaseInstallationIds.isEmpty()) {
            log.debug("알림 수신 대상이 존재하지 않아 알림을 보내지 않습니다.");
            return;
        }

        NotificationRegistrations sentRegistrations = registrationService.snapshotForSend(firebaseInstallationIds);
        FcmSendResult result = pushSender.send(
                firebaseInstallationIds,
                "새로운 댓글이 달렸어요!",
                "댓글을 남겼던 문장의 이야기를 확인해 보세요."
        );
        failureHandler.handle(result, sentRegistrations);
    }
}
