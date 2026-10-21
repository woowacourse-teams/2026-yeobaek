package yeobaek.backend.notification.registration.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.Members;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.dto.NotificationRegistrationRequest;
import yeobaek.backend.notification.registration.repository.NotificationRegistrationRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRegistrationService {
    private final NotificationRegistrationRepository registrationRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public NotificationRegistration register(Long memberId, NotificationRegistrationRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND, "요청 멤버를 찾을 수 없습니다."));

        NotificationRegistration notificationRegistration =
                new NotificationRegistration(member, new FirebaseInstallationId(request.fid()));

        return registrationRepository.save(notificationRegistration);
    }

    @Transactional(readOnly = true)
    public NotificationRegistrations findByMembers(Members members) {
        return new NotificationRegistrations(registrationRepository.findAllByMember_IdIn(members.ids()));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public NotificationRegistrations snapshotForRetry(List<Long> ids, FirebaseInstallationId fid) {
        return new NotificationRegistrations(registrationRepository.findAllByIdInAndFirebaseInstallationId(ids, fid));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteUnregistered(NotificationRegistrations sentRegistrations, FirebaseInstallationId fid) {
        List<Long> ids = sentRegistrations.idsFor(fid);
        if (ids.isEmpty()) {
            return 0;
        }
        return registrationRepository.deleteUnregistered(ids, fid);
    }
}
