package yeobaek.backend.notification.clubjoin;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.club.event.ClubMemberJoinedEvent;
import yeobaek.backend.notification.FcmNotificationService;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

@Service
@RequiredArgsConstructor
public class ClubJoinNotificationService {
    private final FcmNotificationService notificationService;
    private final NotificationRegistrationService registrationService;
    private final ClubJoinNotificationRecipientQueryRepository recipientQueryRepository;

    public void notifyMembers(ClubMemberJoinedEvent event) {
        notificationService.notifyRecipients(NotificationType.CLUB_JOIN, event.clubId(), event.joinedAt(),
                () -> registrationService.findByMemberIds(
                        recipientQueryRepository.findRecipientMemberIds(event.clubId(), event.memberId()),
                        NotificationType.CLUB_JOIN),
                "새로운 모임원이 참여했어요!", "모임에서 함께 이야기를 나눠 보세요.",
                Map.of("type", "CLUB_JOIN", "clubId", event.clubId().toString(),
                        "memberId", event.memberId().toString()));
    }
}
