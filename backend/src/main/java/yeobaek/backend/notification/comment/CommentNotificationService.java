package yeobaek.backend.notification.comment;

import com.google.firebase.messaging.FirebaseMessagingException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yeobaek.backend.notification.FcmMessageFactory;
import yeobaek.backend.notification.FcmPushSender;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentNotificationService {
    private final CommentNotificationRecipientReader recipientReader;
    private final FcmMessageFactory messageFactory;
    private final FcmPushSender pushSender;

    public void notifyReadersInClub(Long clubId, Long sentenceId, Long writerId) throws FirebaseMessagingException {
        Set<String> firebaseInstallationIds = recipientReader.findReadersInClub(clubId, sentenceId, writerId);

        if (firebaseInstallationIds.isEmpty()) {
            log.debug("알림 수신 대상이 존재하지 않아 알림을 보내지 않습니다.");
            return;
        }

        pushSender.send(messageFactory.createMulticastMessage(
                firebaseInstallationIds,
                "댓글이 달렸어요!",
                "확인하러 가기!"));
    }

    public void notifyPreviousCommentersInPublicRoom(Long publicRoomId, Long sentenceId, Long writerId)
            throws FirebaseMessagingException {
        Set<String> firebaseInstallationIds = recipientReader.findPreviousCommentersInPublicRoom(
                publicRoomId, sentenceId, writerId);

        if (firebaseInstallationIds.isEmpty()) {
            log.debug("알림 수신 대상이 존재하지 않아 알림을 보내지 않습니다.");
            return;
        }

        pushSender.send(messageFactory.createMulticastMessage(
                firebaseInstallationIds,
                "새로운 댓글이 달렸어요!",
                "댓글을 남겼던 문장의 이야기를 확인해 보세요."
        ));
    }
}
