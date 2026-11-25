package yeobaek.backend.notification.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationPreference;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesResponse;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesUpdateRequest;
import yeobaek.backend.notification.registration.repository.NotificationPreferenceRepository;

class NotificationPreferencesServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final FirebaseInstallationId FID = new FirebaseInstallationId("c" + "a".repeat(21));

    private NotificationPreferencesService service;

    @BeforeEach
    void setUp() {
        var registration = new NotificationRegistration(2L, new Member(new Nickname("독자")), FID);
        registration.changeEnabled(false);
        var preferences = NotificationPreference.defaultsFor(registration);
        preferences.stream()
                .filter(preference -> preference.hasNotificationType(NotificationType.CLUB_JOIN))
                .forEach(preference -> preference.changeEnabled(false));
        var registrationService = mock(NotificationRegistrationService.class);
        var preferenceRepository = mock(NotificationPreferenceRepository.class);
        given(registrationService.find(MEMBER_ID, FID)).willReturn(registration);
        given(preferenceRepository.findAllByRegistration_Id(registration.getId())).willReturn(preferences);
        service = new NotificationPreferencesService(registrationService, preferenceRepository);
    }

    @Test
    @DisplayName("알림 설정에서 지정한 항목만 변경한다")
    void updateOnlySpecifiedSettings() {
        service.update(MEMBER_ID, FID, new NotificationPreferencesUpdateRequest(null, null, false, null));

        assertThat(service.find(MEMBER_ID, FID))
                .isEqualTo(new NotificationPreferencesResponse(false, false, false, true));
    }

    @Test
    @DisplayName("빈 변경 요청은 기존 알림 설정을 유지한다")
    void emptyUpdatePreservesExistingSettings() {
        service.update(MEMBER_ID, FID, new NotificationPreferencesUpdateRequest(null, null, null, null));

        assertThat(service.find(MEMBER_ID, FID))
                .isEqualTo(new NotificationPreferencesResponse(false, false, true, true));
    }
}
