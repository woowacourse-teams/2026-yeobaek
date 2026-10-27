package yeobaek.backend.notification.registration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationPreference;
import yeobaek.backend.notification.registration.domain.NotificationPreferences;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesResponse;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesUpdateRequest;
import yeobaek.backend.notification.registration.repository.NotificationPreferenceRepository;

@Service
@RequiredArgsConstructor
public class NotificationPreferencesService {
    private final NotificationRegistrationService registrationService;
    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse find(Long memberId, FirebaseInstallationId fid) {
        NotificationRegistration registration = registrationService.find(memberId, fid);
        NotificationPreferences preferences = findPreferences(registration);
        return NotificationPreferencesResponse.from(registration, preferences);
    }

    @Transactional
    public void update(Long memberId, FirebaseInstallationId fid, NotificationPreferencesUpdateRequest request) {
        NotificationRegistration registration = registrationService.find(memberId, fid);
        NotificationPreferences preferences = findPreferences(registration);
        if (request.enabled() != null) {
            registration.changeEnabled(request.enabled());
        }
        preferences.update(request.typeChanges());
    }

    private NotificationPreferences findPreferences(NotificationRegistration registration) {
        List<NotificationPreference> preferences = preferenceRepository.findAllByRegistration_Id(registration.getId());
        if (preferences.isEmpty()) {
            throw new IllegalStateException("등록 정보에 알림 유형별 설정이 없습니다.");
        }
        return new NotificationPreferences(preferences);
    }
}
