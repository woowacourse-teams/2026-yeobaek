package yeobaek.backend.notification.registration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationPreference;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.dto.NotificationRegistrationRequest;
import yeobaek.backend.notification.registration.repository.NotificationPreferenceRepository;
import yeobaek.backend.notification.registration.repository.NotificationRegistrationRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
public class NotificationRegistrationService {
    private final NotificationRegistrationRepository registrationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional
    public NotificationRegistration register(Long memberId, NotificationRegistrationRequest request) {
        FirebaseInstallationId fid = new FirebaseInstallationId(request.fid());
        registrationRepository.insertIfAbsent(memberId, fid.value());
        NotificationRegistration registration = registrationRepository.findByMember_IdAndFirebaseInstallationId(
                        memberId, fid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND, "요청 멤버를 찾을 수 없습니다."));
        if (preferenceRepository.findAllByRegistration_Id(registration.getId()).isEmpty()) {
            preferenceRepository.saveAll(NotificationPreference.defaultsFor(registration));
        }
        return registration;
    }

    @Transactional(readOnly = true)
    public NotificationRegistration find(Long memberId, FirebaseInstallationId fid) {
        return registrationRepository.findByMember_IdAndFirebaseInstallationId(memberId, fid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOTIFICATION_REGISTRATION_NOT_FOUND,
                        "회원의 알림 등록 정보를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public NotificationRegistrations findByMemberIds(List<Long> memberIds, NotificationType type) {
        return NotificationRegistrations.fromEnabledPreferences(
                preferenceRepository.findEnabledWithRegistrationsByMemberIds(memberIds), type);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public NotificationRegistrations snapshotForRetry(List<Long> ids, FirebaseInstallationId fid) {
        return new NotificationRegistrations(registrationRepository.findAllByIdInAndFirebaseInstallationId(ids, fid));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteUnregistered(NotificationRegistrations sentRegistrations, FirebaseInstallationId fid) {
        List<Long> ids = sentRegistrations.findRegistrationIdsByFid(fid);
        if (ids.isEmpty()) {
            return 0;
        }
        return registrationRepository.deleteUnregistered(ids, fid);
    }
}
