package yeobaek.backend.notification.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.member.service.MemberService;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationPreference;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesResponse;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesUpdateRequest;
import yeobaek.backend.notification.registration.dto.NotificationRegistrationRequest;
import yeobaek.backend.notification.registration.repository.NotificationPreferenceRepository;
import yeobaek.backend.notification.registration.repository.NotificationRegistrationRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.support.NotFoundException;

class NotificationPreferencesIntegrationTest extends IntegrationTest {
    private static final FirebaseInstallationId PHONE = new FirebaseInstallationId("c" + "a".repeat(21));
    private static final FirebaseInstallationId TABLET = new FirebaseInstallationId("d" + "a".repeat(21));

    @Autowired
    private NotificationRegistrationService registrations;
    @Autowired
    private NotificationPreferencesService preferences;
    @Autowired
    private NotificationRegistrationRepository registrationRepository;
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;
    @Autowired
    private MemberRepository members;
    @Autowired
    private MemberService memberService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("최초 등록은 모든 유형을 명시적으로 ON으로 저장한다")
    void initializeAllTypes() {
        Member member = member();
        NotificationRegistration registration = register(member, PHONE);

        assertThat(preferences.find(member.getId(), PHONE))
                .isEqualTo(new NotificationPreferencesResponse(true, true, true, true));
        assertThat(preferenceRepository.findAllByRegistration_Id(registration.getId()))
                .extracting(NotificationPreference::getNotificationType)
                .containsExactlyInAnyOrder(NotificationType.values());
        assertThat(jdbc.queryForObject("select count(*) from notification_preferences", Long.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("부분 변경은 다른 항목과 다른 기기 설정을 유지한다")
    void updateOnlyProvidedSettingsOnOneDevice() {
        Member member = member();
        register(member, PHONE);
        register(member, TABLET);

        preferences.update(member.getId(), PHONE, new NotificationPreferencesUpdateRequest(null, null, false, null));

        assertThat(preferences.find(member.getId(), PHONE))
                .isEqualTo(new NotificationPreferencesResponse(true, true, false, true));
        assertThat(preferences.find(member.getId(), TABLET))
                .isEqualTo(new NotificationPreferencesResponse(true, true, true, true));
    }

    @Test
    @DisplayName("전체 OFF 후 ON과 동일 FID 재등록은 기존 유형별 설정을 유지한다")
    void retainTypesAcrossOverallToggleAndRegistration() {
        Member member = member();
        NotificationRegistration original = register(member, PHONE);
        preferences.update(member.getId(), PHONE, new NotificationPreferencesUpdateRequest(false, null, null, false));
        assertThat(preferences.find(member.getId(), PHONE))
                .isEqualTo(new NotificationPreferencesResponse(false, true, true, false));

        NotificationRegistration repeated = register(member, PHONE);
        preferences.update(member.getId(), PHONE, new NotificationPreferencesUpdateRequest(true, null, null, null));

        assertThat(repeated.getId()).isEqualTo(original.getId());
        assertThat(preferences.find(member.getId(), PHONE))
                .isEqualTo(new NotificationPreferencesResponse(true, true, true, false));
        assertThat(registrationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("빈 부분 변경 요청은 기존 설정을 유지한다")
    void ignoreOmittedSettings() {
        Member member = member();
        register(member, PHONE);
        preferences.update(member.getId(), PHONE, new NotificationPreferencesUpdateRequest(null, false, null, null));

        preferences.update(member.getId(), PHONE, new NotificationPreferencesUpdateRequest(null, null, null, null));

        assertThat(preferences.find(member.getId(), PHONE))
                .isEqualTo(new NotificationPreferencesResponse(true, false, true, true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("다른 회원의 FID로 설정을 조회하거나 변경할 수 없다")
    void rejectOtherMembersRegistration(boolean update) {
        Member owner = member();
        register(owner, PHONE);
        Member other = members.save(new Member(new Nickname("다른회원")));

        assertThatThrownBy(() -> {
            if (update) {
                preferences.update(other.getId(), PHONE,
                        new NotificationPreferencesUpdateRequest(false, null, null, null));
            } else {
                preferences.find(other.getId(), PHONE);
            }
        }).isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.NOTIFICATION_REGISTRATION_NOT_FOUND);
        assertThat(preferences.find(owner.getId(), PHONE).enabled()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("설정이 누락되면 전체 설정과 유형별 설정의 변경을 함께 롤백해요")
    void rollbackOverallChangeWhenPreferencesAreMissing(boolean allMissing) {
        Member member = member();
        NotificationRegistration registration = register(member, PHONE);
        if (allMissing) {
            jdbc.update("delete from notification_preferences where registration_id = ?", registration.getId());
        } else {
            jdbc.update("delete from notification_preferences where registration_id = ? and notification_type = ?",
                    registration.getId(), NotificationType.CLUB_COMMENT.name());
        }

        assertThatThrownBy(() -> preferences.update(member.getId(), PHONE,
                new NotificationPreferencesUpdateRequest(false, false, false, null)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(registrationRepository.findById(registration.getId()).orElseThrow().isEnabled()).isTrue();
        assertThat(preferenceRepository.findAllByRegistration_Id(registration.getId()))
                .allMatch(NotificationPreference::isEnabled);
    }

    @Test
    @DisplayName("설정 조회는 등록을 중복 조회하지 않고 두 SQL로 완료해요")
    void findSettingsWithoutReloadingRegistration() {
        Member member = member();
        register(member, PHONE);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(preferences.find(member.getId(), PHONE))
                    .isEqualTo(new NotificationPreferencesResponse(true, true, true, true));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        } finally {
            statistics.setStatisticsEnabled(previous);
        }
    }

    @Test
    @DisplayName("회원 탈퇴는 모든 기기의 등록과 설정을 삭제하고 다른 회원은 유지한다")
    void deleteAllDevicesOnWithdrawal() {
        Member member = member();
        register(member, PHONE);
        register(member, TABLET);
        Member other = members.save(new Member(new Nickname("다른회원")));
        NotificationRegistration retained = register(other, PHONE);

        memberService.delete(member.getId());

        assertThat(registrationRepository.findAll()).extracting(NotificationRegistration::getId)
                .containsExactly(retained.getId());
        assertThat(jdbc.queryForObject("select count(*) from notification_preferences", Long.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("등록과 전체 ON 기기의 모든 설정을 한 SQL로 읽고 애플리케이션에서 유형별 필터링한다")
    void joinSettingsInOneQuery() {
        Member member = member();
        register(member, PHONE);
        register(member, TABLET);
        preferences.update(member.getId(), TABLET, new NotificationPreferencesUpdateRequest(false, null, null, null));
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            var snapshot = registrations.findByMemberIds(List.of(member.getId()), NotificationType.CLUB_COMMENT);

            assertThat(snapshot.firebaseInstallationIds())
                    .containsExactly(PHONE.value());
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        } finally {
            statistics.setStatisticsEnabled(previous);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "CLUB_JOIN,false,true,true,true",
            "CLUB_JOIN,true,false,true,true",
            "CLUB_COMMENT,true,true,false,true",
            "PUBLIC_ROOM_COMMENT,true,true,true,false"
    })
    @DisplayName("최초 선정 후 전체 또는 해당 유형을 OFF로 바꾸면 재시도 대상에서 제외해요")
    void excludeDisabledRetryTarget(NotificationType type, boolean enabled, boolean clubJoin,
                                    boolean clubComment, boolean publicRoomComment) {
        Member member = member();
        NotificationRegistration original = register(member, PHONE);
        var selected = registrations.findByMemberIds(List.of(member.getId()), type);
        assertThat(selected.findRegistrationIdsByFid(PHONE)).containsExactly(original.getId());
        preferences.update(member.getId(), PHONE,
                new NotificationPreferencesUpdateRequest(enabled, clubJoin, clubComment, publicRoomComment));

        var snapshot = registrations.snapshotForRetry(selected.findRegistrationIdsByFid(PHONE), PHONE, type);

        assertThat(snapshot.firebaseInstallationIds()).isEmpty();
        assertThat(registrationRepository.findById(original.getId())).isPresent();
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    @DisplayName("재시도는 원래 등록과 해당 유형 설정만 한 SQL로 확인해요")
    void retryOnlyOriginalRegistrationAndTypeInOneQuery(NotificationType type) {
        Member member = member();
        NotificationRegistration original = register(member, PHONE);
        NotificationRegistration tablet = register(member, TABLET);
        Member other = members.save(new Member(new Nickname("다른회원")));
        register(other, PHONE);
        var settings = preferenceRepository.findAllByRegistration_Id(original.getId());
        settings.stream().filter(setting -> !setting.hasNotificationType(type))
                .forEach(setting -> setting.changeEnabled(false));
        preferenceRepository.saveAll(settings);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            var snapshot = registrations.snapshotForRetry(List.of(original.getId(), tablet.getId()), PHONE, type);

            assertThat(snapshot.findRegistrationIdsByFid(PHONE)).containsExactly(original.getId());
            assertThat(snapshot.firebaseInstallationIds()).containsExactly(PHONE.value());
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        } finally {
            statistics.setStatisticsEnabled(previous);
        }
    }

    @Test
    @DisplayName("동시 재등록에도 회원·FID별 등록과 유형별 설정이 한 번만 생성된다")
    void registerConcurrently() throws InterruptedException, ExecutionException, TimeoutException {
        Member member = member();
        var ready = new CountDownLatch(4);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Future<Long>> futures = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return register(member, PHONE).getId();
                }));
            }
            boolean allReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertThat(allReady).isTrue();
            List<Long> ids = new ArrayList<>();
            for (Future<Long> future : futures) {
                ids.add(future.get(10, TimeUnit.SECONDS));
            }
            assertThat(ids).containsOnly(ids.getFirst());
        }
        assertThat(registrationRepository.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from notification_preferences", Long.class)).isEqualTo(3);
    }

    private Member member() {
        return members.save(new Member(new Nickname("알림회원")));
    }

    private NotificationRegistration register(Member member, FirebaseInstallationId fid) {
        return registrations.register(member.getId(), new NotificationRegistrationRequest(fid.value()));
    }
}
