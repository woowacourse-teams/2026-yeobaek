package yeobaek.backend.notification.registration.service;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.notification.domain.ClubCommentNotificationContext;
import yeobaek.backend.notification.domain.ClubJoinNotificationContext;
import yeobaek.backend.notification.domain.NotificationContext;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.domain.PublicRoomCommentNotificationContext;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.repository.NotificationPreferenceRepository;

@Service
@RequiredArgsConstructor
public class NotificationRecipientQueryService {
    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public NotificationRegistrations findPublicRoomRecipients(List<Long> memberIds) {
        return NotificationRegistrations.fromEnabledPreferences(
                preferenceRepository.findEnabledWithRegistrationsByMemberIds(memberIds),
                NotificationType.PUBLIC_ROOM_COMMENT);
    }

    @Transactional(readOnly = true)
    public NotificationRegistrations findClubRecipients(List<Long> memberIds, NotificationType type, Long clubId) {
        ensureClubNotification(type, clubId);
        return NotificationRegistrations.fromEnabledPreferences(
                preferenceRepository.findEnabledClubRegistrationsByMemberIds(memberIds, clubId), type);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public NotificationRegistrations snapshotForRetry(List<Long> ids, FirebaseInstallationId fid,
                                                     NotificationContext context) {
        return switch (context) {
            case ClubJoinNotificationContext club -> snapshotForClubRetry(ids, fid, club.notificationType(), club.clubId());
            case ClubCommentNotificationContext club -> snapshotForClubRetry(ids, fid, club.notificationType(), club.clubId());
            case PublicRoomCommentNotificationContext ignored -> snapshotForPublicRoomRetry(ids, fid);
        };
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public NotificationRegistrations snapshotForPublicRoomRetry(List<Long> ids, FirebaseInstallationId fid) {
        return NotificationRegistrations.fromEnabledPreferences(
                preferenceRepository.findEnabledWithRegistrationsByRegistrationIdsAndFid(ids, fid),
                NotificationType.PUBLIC_ROOM_COMMENT);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public NotificationRegistrations snapshotForClubRetry(List<Long> ids, FirebaseInstallationId fid,
                                                         NotificationType type, Long clubId) {
        ensureClubNotification(type, clubId);
        return NotificationRegistrations.fromEnabledPreferences(
                preferenceRepository.findEnabledClubRegistrationsByRegistrationIdsAndFid(ids, fid, clubId), type);
    }

    private void ensureClubNotification(NotificationType type, Long clubId) {
        Objects.requireNonNull(clubId);
        if (type != NotificationType.CLUB_JOIN && type != NotificationType.CLUB_COMMENT) {
            throw new IllegalArgumentException("모임 가입 또는 모임 댓글 알림만 조회할 수 있습니다.");
        }
    }
}
