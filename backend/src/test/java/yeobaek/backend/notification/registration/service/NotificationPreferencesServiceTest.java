package yeobaek.backend.notification.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationPreference;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesResponse;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesUpdateRequest;
import yeobaek.backend.notification.registration.repository.NotificationPreferenceRepository;

class NotificationPreferencesServiceTest {

    @Test
    @DisplayName("알림 설정에서 생략한 항목은 유지하고 지정한 항목만 변경한다")
    void keepUnspecifiedPreferencesWhenUpdating() {
        long memberId = 1L;
        long registrationId = 2L;
        var fid = new FirebaseInstallationId("c" + "a".repeat(21));
        var registration = new NotificationRegistration(registrationId, new Member(new Nickname("독자")), fid);
        var registrationService = mock(NotificationRegistrationService.class);
        var preferenceRepository = mock(NotificationPreferenceRepository.class);
        given(registrationService.find(memberId, fid)).willReturn(registration);
        given(preferenceRepository.findAllByRegistration_Id(registrationId))
                .willReturn(NotificationPreference.defaultsFor(registration));
        var service = new NotificationPreferencesService(registrationService, preferenceRepository);

        service.update(memberId, fid, new NotificationPreferencesUpdateRequest(false, null, null, null));
        service.update(memberId, fid, new NotificationPreferencesUpdateRequest(null, null, false, null));
        service.update(memberId, fid, new NotificationPreferencesUpdateRequest(null, null, null, null));

        assertThat(service.find(memberId, fid))
                .isEqualTo(new NotificationPreferencesResponse(false, true, false, true));
    }
}
