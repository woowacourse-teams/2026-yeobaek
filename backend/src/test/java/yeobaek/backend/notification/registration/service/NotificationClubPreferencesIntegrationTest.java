package yeobaek.backend.notification.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.dto.NotificationClubPreferenceUpdateRequest;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesUpdateRequest;
import yeobaek.backend.notification.registration.dto.NotificationRegistrationRequest;
import yeobaek.backend.notification.registration.repository.NotificationClubPreferenceRepository;
import yeobaek.backend.notification.registration.repository.NotificationRegistrationRepository;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.ForbiddenException;
import yeobaek.backend.support.IntegrationTest;
import yeobaek.backend.support.NotFoundException;

class NotificationClubPreferencesIntegrationTest extends IntegrationTest {
    private static final FirebaseInstallationId PHONE = new FirebaseInstallationId("c" + "a".repeat(21));
    private static final FirebaseInstallationId TABLET = new FirebaseInstallationId("d" + "a".repeat(21));

    @Autowired
    private NotificationRegistrationService registrations;
    @Autowired
    private NotificationPreferencesService preferences;
    @Autowired
    private NotificationClubPreferencesService clubPreferences;
    @Autowired
    private NotificationClubPreferenceRepository clubPreferenceRepository;
    @Autowired
    private NotificationRegistrationRepository registrationRepository;
    @Autowired
    private MemberRepository members;
    @Autowired
    private JdbcTemplate jdbc;

    private Member member;
    private Long clubId;
    private NotificationRegistration phone;

    @BeforeEach
    void setUp() {
        member = members.save(new Member(new Nickname("모임 알림 회원")));
        clubId = joinClub("CLUB01");
        phone = register(PHONE);
    }

    @Test
    @DisplayName("설정이 없으면 ON을 반환하고 조회만으로 행을 생성하지 않아요")
    void defaultWithoutWriting() {
        assertThat(clubPreferences.find(member.getId(), PHONE, clubId).enabled()).isTrue();
        assertThat(clubPreferenceRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("첫 변경을 저장하고 같은 키의 다음 변경은 기존 행을 갱신해요")
    void createAndUpdatePreference(boolean enabled) {
        change(PHONE, clubId, enabled);
        var original = clubPreferenceRepository.findByRegistrationIdAndClubId(phone.getId(), clubId).orElseThrow();

        change(PHONE, clubId, !enabled);
        var updated = clubPreferenceRepository.findByRegistrationIdAndClubId(phone.getId(), clubId).orElseThrow();

        assertThat(updated.getId()).isEqualTo(original.getId());
        assertThat(updated.isEnabled()).isEqualTo(!enabled);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(original.getUpdatedAt());
        assertThat(clubPreferenceRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationType.class, names = {"CLUB_JOIN", "CLUB_COMMENT"})
    @DisplayName("같은 회원의 다른 설치와 다른 모임은 유지하고 OFF 설치만 제외해요")
    void isolateInstallationAndClub(NotificationType type) {
        register(TABLET);
        Long otherClubId = joinClub("CLUB02");
        change(PHONE, clubId, false);

        assertThat(registrations.findByMemberIdsForClub(List.of(member.getId()), type, clubId)
                .firebaseInstallationIds()).containsExactly(TABLET.value());
        assertThat(registrations.findByMemberIdsForClub(List.of(member.getId()), type, otherClubId)
                .firebaseInstallationIds()).containsExactlyInAnyOrder(PHONE.value(), TABLET.value());
        assertThat(clubPreferences.find(member.getId(), TABLET, clubId).enabled()).isTrue();
        assertThat(clubPreferences.find(member.getId(), PHONE, otherClubId).enabled()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"false,true,true", "true,false,true", "true,true,false"})
    @DisplayName("모임 설정이 ON이어도 전체 또는 해당 유형이 OFF이면 발송하지 않아요")
    void retainOverallAndTypeFilters(boolean overall, boolean clubJoin, boolean clubComment) {
        change(PHONE, clubId, true);
        preferences.update(member.getId(), PHONE,
                new NotificationPreferencesUpdateRequest(overall, clubJoin, clubComment, null));
        NotificationType type = !clubComment ? NotificationType.CLUB_COMMENT : NotificationType.CLUB_JOIN;

        assertThat(registrations.findByMemberIdsForClub(List.of(member.getId()), type, clubId)
                .firebaseInstallationIds()).isEmpty();
        assertThat(clubPreferences.find(member.getId(), PHONE, clubId).enabled()).isTrue();
    }

    @Test
    @DisplayName("모임 알림 OFF는 공개방 댓글 알림에 영향을 주지 않아요")
    void retainPublicRoomNotification() {
        change(PHONE, clubId, false);

        assertThat(registrations.findByMemberIds(List.of(member.getId()), NotificationType.PUBLIC_ROOM_COMMENT)
                .firebaseInstallationIds()).containsExactly(PHONE.value());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("최초 발송 후 모임 설정을 끄거나 탈퇴하면 재시도 대상에서 제외해요")
    void recheckClubBeforeRetry(boolean disablePreference) {
        var selected = registrations.findByMemberIdsForClub(
                List.of(member.getId()), NotificationType.CLUB_COMMENT, clubId);
        assertThat(selected.findRegistrationIdsByFid(PHONE)).containsExactly(phone.getId());
        if (disablePreference) {
            change(PHONE, clubId, false);
        } else {
            jdbc.update("update club_members set status = 'LEFT' where member_id = ? and club_id = ?",
                    member.getId(), clubId);
        }

        assertThat(registrations.snapshotForRetry(selected.findRegistrationIdsByFid(PHONE), PHONE,
                NotificationType.CLUB_COMMENT, clubId).firebaseInstallationIds()).isEmpty();
    }

    @Test
    @DisplayName("재시도는 새 등록이나 다른 FID를 추가하지 않아요")
    void retryOnlyOriginalRegistration() {
        register(TABLET);
        registrationRepository.deleteById(phone.getId());
        var replacement = register(PHONE);

        assertThat(replacement.getId()).isNotEqualTo(phone.getId());
        assertThat(registrations.snapshotForRetry(List.of(phone.getId()), PHONE,
                NotificationType.CLUB_COMMENT, clubId).firebaseInstallationIds()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("다른 회원의 설치는 조회하거나 변경할 수 없어요")
    void rejectOtherMembersInstallation(boolean update) {
        var other = members.save(new Member(new Nickname("다른 회원")));

        assertThatThrownBy(() -> {
            if (update) {
                clubPreferences.update(other.getId(), PHONE, clubId, new NotificationClubPreferenceUpdateRequest(false));
            } else {
                clubPreferences.find(other.getId(), PHONE, clubId);
            }
        }).isInstanceOf(NotFoundException.class)
                .extracting("code").isEqualTo(ErrorCode.NOTIFICATION_REGISTRATION_NOT_FOUND);
        assertThat(clubPreferenceRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("탈퇴한 회원은 설정을 조회하거나 변경할 수 없어요")
    void rejectLeftMember(boolean update) {
        jdbc.update("update club_members set status = 'LEFT' where member_id = ? and club_id = ?",
                member.getId(), clubId);

        assertThatThrownBy(() -> {
            if (update) {
                change(PHONE, clubId, false);
            } else {
                clubPreferences.find(member.getId(), PHONE, clubId);
            }
        }).isInstanceOf(ForbiddenException.class).extracting("code").isEqualTo(ErrorCode.NOT_CLUB_MEMBER);
        assertThat(clubPreferenceRepository.count()).isZero();
    }

    @Test
    @DisplayName("재등록과 모임 탈퇴 후 재가입은 기존 모임 설정을 유지해요")
    void retainPreferenceAcrossRejoinAndRegistration() {
        change(PHONE, clubId, false);
        jdbc.update("update club_members set status = 'LEFT' where member_id = ? and club_id = ?",
                member.getId(), clubId);
        register(PHONE);
        jdbc.update("update club_members set status = 'JOINED' where member_id = ? and club_id = ?",
                member.getId(), clubId);

        assertThat(clubPreferences.find(member.getId(), PHONE, clubId).enabled()).isFalse();
        assertThat(clubPreferenceRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("설치 또는 모임 삭제 시 해당 설정만 FK로 함께 삭제해요")
    void cascadeOnlyRelatedPreferences(boolean deleteRegistration) {
        var tablet = register(TABLET);
        Long otherClubId = joinClub("CLUB02");
        change(PHONE, clubId, false);
        change(TABLET, otherClubId, false);
        if (deleteRegistration) {
            registrationRepository.deleteById(phone.getId());
        } else {
            jdbc.update("delete from club_members where club_id = ?", clubId);
            jdbc.update("delete from clubs where id = ?", clubId);
        }

        assertThat(clubPreferenceRepository.findAll()).singleElement().satisfies(setting -> {
            assertThat(setting.getRegistrationId()).isEqualTo(tablet.getId());
            assertThat(setting.getClubId()).isEqualTo(otherClubId);
        });
    }

    private Long joinClub(String code) {
        jdbc.update("insert into clubs (name, join_code) values (?, ?)", "알림 모임", code);
        Long id = jdbc.queryForObject("select id from clubs where join_code = ?", Long.class, code);
        jdbc.update("insert into club_members (member_id, club_id, status) values (?, ?, 'JOINED')", member.getId(), id);
        return id;
    }

    private NotificationRegistration register(FirebaseInstallationId fid) {
        return registrations.register(member.getId(), new NotificationRegistrationRequest(fid.value()));
    }

    private void change(FirebaseInstallationId fid, Long targetClubId, boolean enabled) {
        clubPreferences.update(member.getId(), fid, targetClubId, new NotificationClubPreferenceUpdateRequest(enabled));
    }
}
