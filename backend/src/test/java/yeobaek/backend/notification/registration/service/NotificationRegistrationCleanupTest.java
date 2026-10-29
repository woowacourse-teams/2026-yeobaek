package yeobaek.backend.notification.registration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationPreference;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.dto.NotificationRegistrationRequest;
import yeobaek.backend.notification.registration.repository.NotificationPreferenceRepository;
import yeobaek.backend.notification.registration.repository.NotificationRegistrationRepository;
import yeobaek.backend.support.IntegrationTest;

class NotificationRegistrationCleanupTest extends IntegrationTest {

    @Autowired
    private NotificationRegistrationService registrationService;

    @Autowired
    private NotificationRegistrationRepository registrationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("발송 전 확인한 등록만 삭제하고 다른 회원의 같은 FID 등록과 다른 기기를 유지한다")
    void preserveNewRegistrationAndOtherDevices() {
        Member member = memberRepository.save(new Member(new Nickname("독자")));
        var phone = save(member, 'c');
        var tablet = save(member, 'd');
        Member other = memberRepository.save(new Member(new Nickname("다른독자")));
        var otherPhone = save(other, 'c');
        var snapshot = registrationService.findByMemberIds(List.of(member.getId()), NotificationType.CLUB_COMMENT);

        int deletedCount = registrationService.deleteUnregistered(snapshot, phone.getFirebaseInstallationId());

        assertThat(deletedCount).isEqualTo(1);
        assertThat(registrationRepository.findAll()).extracting(NotificationRegistration::getId)
                .containsExactlyInAnyOrder(tablet.getId(), otherPhone.getId());
    }

    @Test
    @DisplayName("반복 등록은 같은 행을 유지하고 등록 실패 정리는 연결된 설정도 삭제한다")
    void deleteRepeatedRegistrationWithPreferences() {
        Member member = memberRepository.save(new Member(new Nickname("독자")));
        var phone = save(member, 'c');
        var repeated = registrationService.register(member.getId(),
                new NotificationRegistrationRequest(phone.getFirebaseInstallationId().value()));
        var snapshot = registrationService.findByMemberIds(List.of(member.getId()), NotificationType.CLUB_COMMENT);

        int deletedCount = registrationService.deleteUnregistered(snapshot, phone.getFirebaseInstallationId());

        assertThat(repeated.getId()).isEqualTo(phone.getId());
        assertThat(deletedCount).isEqualTo(1);
        assertThat(registrationRepository.findAll()).isEmpty();
        assertThat(jdbcTemplate.queryForObject("select count(*) from notification_preferences", Long.class)).isZero();
    }

    @Test
    @DisplayName("발송 후 같은 행의 FID가 변경됐으면 이전 실패로 삭제하지 않는다")
    void preserveChangedFid() {
        Member member = memberRepository.save(new Member(new Nickname("독자")));
        var phone = save(member, 'c');
        var snapshot = registrationService.findByMemberIds(List.of(member.getId()), NotificationType.CLUB_COMMENT);
        String changedFid = "d" + "a".repeat(21);
        jdbcTemplate.update("update notification_registrations set fid = ? where id = ?", changedFid, phone.getId());

        int deletedCount = registrationService.deleteUnregistered(snapshot, phone.getFirebaseInstallationId());

        assertThat(deletedCount).isZero();
        assertThat(registrationRepository.findById(phone.getId())).get()
                .extracting(registration -> registration.getFirebaseInstallationId().value()).isEqualTo(changedFid);
    }

    @Test
    @DisplayName("커밋 후 콜백에서 호출해도 새 트랜잭션으로 등록 삭제를 커밋한다")
    void commitCleanupAfterOriginalTransaction() {
        Member member = memberRepository.save(new Member(new Nickname("독자")));
        var phone = save(member, 'c');
        var snapshot = registrationService.findByMemberIds(List.of(member.getId()), NotificationType.CLUB_COMMENT);

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        registrationService.deleteUnregistered(snapshot, phone.getFirebaseInstallationId());
                    }
                }));

        assertThat(registrationRepository.findById(phone.getId())).isEmpty();
    }

    @Test
    @DisplayName("발송 당시 등록이 없으면 나중에 들어온 동일 FID 등록을 삭제하지 않는다")
    void preserveRegistrationAbsentFromSnapshot() {
        Member member = memberRepository.save(new Member(new Nickname("독자")));
        var phone = save(member, 'c');
        var emptySnapshot = new NotificationRegistrations(List.of());

        int deletedCount = registrationService.deleteUnregistered(emptySnapshot, phone.getFirebaseInstallationId());

        assertThat(deletedCount).isZero();
        assertThat(registrationRepository.findById(phone.getId())).isPresent();
    }

    private NotificationRegistration save(Member member, char prefix) {
        var registration = registrationRepository.save(new NotificationRegistration(
                member, new FirebaseInstallationId(prefix + "a".repeat(21))));
        preferenceRepository.saveAll(NotificationPreference.defaultsFor(registration));
        return registration;
    }

    @Test
    @DisplayName("재시도 조회는 원래 등록 ID와 FID를 모두 확인해 같은 FID의 새 등록을 가져오지 않는다")
    void excludeReplacementRegistrationFromRetry() {
        Member member = memberRepository.save(new Member(new Nickname("독자")));
        var phone = save(member, 'c');
        var tablet = save(member, 'd');
        List<Long> originalIds = List.of(phone.getId());
        registrationRepository.deleteById(phone.getId());
        var replacement = save(member, 'c');

        var snapshot = registrationService.snapshotForRetry(
                originalIds, phone.getFirebaseInstallationId(), NotificationType.CLUB_COMMENT);
        int deleted = registrationService.deleteUnregistered(
                new NotificationRegistrations(List.of(phone)), phone.getFirebaseInstallationId());

        assertThat(snapshot.findRegistrationIdsByFid(phone.getFirebaseInstallationId())).isEmpty();
        assertThat(deleted).isZero();
        assertThat(registrationRepository.findAll()).extracting(NotificationRegistration::getId)
                .containsExactlyInAnyOrder(tablet.getId(), replacement.getId());
    }

    @Test
    @DisplayName("원래 등록 행의 FID가 변경되면 재시도 대상에서 제외한다")
    void excludeChangedFidFromRetry() {
        Member member = memberRepository.save(new Member(new Nickname("독자")));
        var phone = save(member, 'c');
        jdbcTemplate.update("update notification_registrations set fid = ? where id = ?",
                "d" + "a".repeat(21), phone.getId());

        var snapshot = registrationService.snapshotForRetry(
                List.of(phone.getId()), phone.getFirebaseInstallationId(), NotificationType.CLUB_COMMENT);

        assertThat(snapshot.findRegistrationIdsByFid(phone.getFirebaseInstallationId())).isEmpty();
        assertThat(registrationRepository.findById(phone.getId())).isPresent();
    }
}
