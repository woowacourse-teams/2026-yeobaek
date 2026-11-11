package yeobaek.backend.notification.clubjoin;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.club.event.ClubMemberJoinedEvent;
import yeobaek.backend.notification.FcmNotificationService;
import yeobaek.backend.notification.domain.ClubJoinNotificationContext;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRecipientQueryService;

@Service
@RequiredArgsConstructor
public class ClubJoinNotificationService {
    private final FcmNotificationService notificationService;
    private final NotificationRecipientQueryService recipientQueryService;
    private final ClubJoinNotificationRecipientQueryRepository recipientQueryRepository;

    public void notifyMembers(ClubMemberJoinedEvent event) {
        var context = new ClubJoinNotificationContext(event.clubId(), event.joinedAt());
        Supplier<NotificationRegistrations> recipients = () -> findRecipients(context, event.memberId());
        String title = "새로운 모임원이 참여했어요!";
        String body = "모임에서 함께 이야기를 나눠 보세요.";
        Map<String, String> data = Map.of("type", context.notificationType().name(), "clubId", context.clubId().toString(),
                "memberId", event.memberId().toString());
        notificationService.notifyRecipients(context, recipients, title, body, data);
    }

    private NotificationRegistrations findRecipients(ClubJoinNotificationContext context, Long joiningMemberId) {
        List<Long> candidateMemberIds = recipientQueryRepository.findRecipientMemberIds(context.clubId(), joiningMemberId);
        return recipientQueryService.findClubRecipients(candidateMemberIds, context.notificationType(), context.clubId());
    }
}
