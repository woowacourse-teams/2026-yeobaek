package yeobaek.backend.notification.registration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationClubPreference;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.dto.NotificationClubPreferenceResponse;
import yeobaek.backend.notification.registration.dto.NotificationClubPreferenceUpdateRequest;
import yeobaek.backend.notification.registration.repository.NotificationClubPreferenceRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;

@Service
@RequiredArgsConstructor
public class NotificationClubPreferencesService {
    private final NotificationRegistrationService registrationService;
    private final ClubMemberRepository clubMemberRepository;
    private final NotificationClubPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public NotificationClubPreferenceResponse find(Long memberId, FirebaseInstallationId fid, Long clubId) {
        NotificationRegistration registration = registrationService.find(memberId, fid);
        ensureJoined(memberId, clubId);
        boolean enabled = preferenceRepository.findByRegistrationIdAndClubId(registration.getId(), clubId)
                .map(NotificationClubPreference::isEnabled).orElse(true);
        return new NotificationClubPreferenceResponse(enabled);
    }

    @Transactional
    public void update(Long memberId, FirebaseInstallationId fid, Long clubId,
                       NotificationClubPreferenceUpdateRequest request) {
        NotificationRegistration registration = registrationService.find(memberId, fid);
        ensureJoined(memberId, clubId);
        NotificationClubPreference preference =
                new NotificationClubPreference(registration.getId(), clubId, request.enabled());
        preferenceRepository.upsert(preference.getRegistrationId(), preference.getClubId(),
                preference.isEnabled(), preference.getUpdatedAt());
    }

    private void ensureJoined(Long memberId, Long clubId) {
        if (!clubMemberRepository.existsJoinedByMemberIdAndClubId(memberId, clubId)) {
            throw new ForbiddenException(ErrorCode.NOT_CLUB_MEMBER, "모임에 참여 중인 회원이 아닙니다.");
        }
    }
}
