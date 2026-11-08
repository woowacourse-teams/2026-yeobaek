package yeobaek.backend.notification.comment;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.comment.event.ClubSentenceCommentCreatedEvent;
import yeobaek.backend.comment.event.PublicRoomSentenceCommentCreatedEvent;
import yeobaek.backend.notification.FcmNotificationService;
import yeobaek.backend.notification.domain.ClubCommentNotificationContext;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.domain.PublicRoomCommentNotificationContext;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRecipientQueryService;

@Service
@RequiredArgsConstructor
public class CommentNotificationService {
    private static final String TITLE = "새로운 댓글이 달렸어요!";
    private static final String BODY = "댓글을 남겼던 문장의 이야기를 확인해 보세요.";

    private final FcmNotificationService notificationService;
    private final NotificationRecipientQueryService recipientQueryService;
    private final CommentNotificationRecipientQueryRepository recipientQueryRepository;

    public void notifyClub(ClubSentenceCommentCreatedEvent event) {
        Map<String, String> data = Map.of(
                "type", "CLUB_COMMENT", "clubId", event.clubId().toString(),
                "passageSequence", Integer.toString(event.passageSequence()), "sentenceId", event.sentenceId().toString());
        var context = new ClubCommentNotificationContext(event.commentId(), event.clubId(), event.commentCreatedAt());
        Supplier<NotificationRegistrations> recipients = () -> findReadersInClub(
                event.clubId(), new ContentSequence(event.passageSequence()), event.writerId());
        notificationService.notifyRecipients(context, recipients, TITLE, BODY, data);
    }

    public void notifyPublicRoom(PublicRoomSentenceCommentCreatedEvent event) {
        Map<String, String> data = Map.of(
                "type", "PUBLIC_ROOM_COMMENT", "publicRoomId", event.publicRoomId().toString(),
                "passageSequence", Integer.toString(event.passageSequence()), "sentenceId", event.sentenceId().toString());
        var context = new PublicRoomCommentNotificationContext(event.commentId(), event.commentCreatedAt());
        Supplier<NotificationRegistrations> recipients = () -> findPreviousCommentersInPublicRoom(
                event.publicRoomId(), event.sentenceId(), event.writerId());
        notificationService.notifyRecipients(context, recipients, TITLE, BODY, data);
    }

    private NotificationRegistrations findReadersInClub(Long clubId, ContentSequence passageSequence, Long writerId) {
        List<Long> recipientIds = recipientQueryRepository.findReaderIdsAheadInClub(clubId, passageSequence.value(), writerId);
        return recipientQueryService.findClubRecipients(recipientIds, NotificationType.CLUB_COMMENT, clubId);
    }

    private NotificationRegistrations findPreviousCommentersInPublicRoom(Long publicRoomId, Long sentenceId, Long writerId) {
        List<Long> recipientIds = recipientQueryRepository
                .findPreviousCommenterIdsInPublicRoom(publicRoomId, sentenceId, writerId);
        return recipientQueryService.findPublicRoomRecipients(recipientIds);
    }
}
