package yeobaek.backend.notification.clubjoin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessagingException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.repository.BookManagementRepository;
import yeobaek.backend.club.domain.Club;
import yeobaek.backend.club.domain.ClubMember;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.club.domain.vo.JoinCode;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.club.service.ClubService;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.member.domain.vo.Nickname;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.FcmRetryService;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.domain.NotificationType;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.dto.NotificationPreferencesUpdateRequest;
import yeobaek.backend.notification.registration.dto.NotificationRegistrationRequest;
import yeobaek.backend.notification.registration.repository.NotificationRegistrationRepository;
import yeobaek.backend.notification.registration.service.NotificationPreferencesService;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;
import yeobaek.backend.support.IntegrationTest;

class ClubJoinNotificationIntegrationTest extends IntegrationTest {
    private static final JoinCode JOIN_CODE = new JoinCode("JOIN02");
    private static final String PHONE = "c" + "a".repeat(21);
    private static final String TABLET = "d" + "a".repeat(21);
    private static final String TITLE = "새로운 모임원이 참여했어요!";
    private static final String BODY = "모임에서 함께 이야기를 나눠 보세요.";

    @Autowired
    private ClubService clubService;
    @Autowired
    private BookManagementRepository books;
    @Autowired
    private ClubRepository clubs;
    @Autowired
    private ClubMemberRepository memberships;
    @Autowired
    private MemberRepository members;
    @Autowired
    private NotificationRegistrationService registrations;
    @Autowired
    private NotificationRegistrationRepository registrationRepository;
    @Autowired
    private NotificationPreferencesService preferences;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private FcmPushSender sender;
    @Autowired
    private FcmRetryService retryService;

    private Club club;
    private Member recipient;
    private Member joiner;

    @BeforeEach
    void setUp() throws FirebaseMessagingException {
        Book book = books.save(new Book(new BookTitle("입장 알림 도서"), null, null, 1, null));
        club = clubs.save(new Club(new ClubName("입장 알림 모임"), book, JOIN_CODE));
        recipient = members.save(new Member(new Nickname("기존 구성원")));
        memberships.save(new ClubMember(recipient, club));
        joiner = members.save(new Member(new Nickname("입장자")));
        register(recipient, PHONE);
        given(sender.send(any(), any(), any(), any())).willReturn(new FcmSendResult(1, List.of()));
    }

    @Test
    @DisplayName("가입 커밋 후 기존 구성원에게 보내고 입장자 본인의 기기는 제외해요")
    void sendAfterJoiningAndExcludeOwnDevices() throws FirebaseMessagingException {
        register(joiner, "e" + "a".repeat(21));

        clubService.join(joiner.getId(), JOIN_CODE);

        verify(sender).send(List.of(PHONE), TITLE, BODY, data());
        assertThat(memberships.existsJoinedByMemberIdAndClubId(joiner.getId(), club.getId())).isTrue();
    }

    @Test
    @DisplayName("재가입은 알리고 이미 가입 중인 회원의 중복 요청은 추가 알림을 보내지 않아요")
    void notifyRejoinButIgnoreRepeatedJoin() throws FirebaseMessagingException {
        clubService.join(joiner.getId(), JOIN_CODE);
        clubService.leave(joiner.getId(), club.getId());
        clearInvocations(sender);

        clubService.join(joiner.getId(), JOIN_CODE);
        clubService.join(joiner.getId(), JOIN_CODE);

        verify(sender, times(1)).send(List.of(PHONE), TITLE, BODY, data());
        assertThat(memberships.findByMemberIdAndClubId(joiner.getId(), club.getId())).isPresent();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("전체 알림이나 모임 입장 알림을 끈 기기만 제외하고 다른 기기는 유지해요")
    void applyDevicePreferences(boolean disableOverall) throws FirebaseMessagingException {
        register(recipient, TABLET);
        preferences.update(recipient.getId(), new FirebaseInstallationId(PHONE),
                new NotificationPreferencesUpdateRequest(disableOverall ? false : null,
                        disableOverall ? null : false, null, null));

        clubService.join(joiner.getId(), JOIN_CODE);

        verify(sender).send(List.of(TABLET), TITLE, BODY, data());
    }

    @Test
    @DisplayName("댓글 알림만 꺼도 모임 입장 알림은 발송해요")
    void retainJoinPreferenceWhenCommentDisabled() throws FirebaseMessagingException {
        preferences.update(recipient.getId(), new FirebaseInstallationId(PHONE),
                new NotificationPreferencesUpdateRequest(null, null, false, false));

        clubService.join(joiner.getId(), JOIN_CODE);

        verify(sender).send(List.of(PHONE), TITLE, BODY, data());
    }

    @Test
    @DisplayName("가입 트랜잭션이 롤백되면 등록과 알림 발송을 모두 취소해요")
    void skipNotificationAfterRollback() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            clubService.join(joiner.getId(), JOIN_CODE);
            throw new IllegalStateException("rollback join");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(memberships.findByMemberIdAndClubId(joiner.getId(), club.getId())).isEmpty();
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("등록 해제는 정리하고 재시도 가능한 실패만 모임 ID와 입장 시각으로 예약해요")
    void cleanupAndRetryOnlyFailedDevices() throws FirebaseMessagingException {
        NotificationRegistration tablet = register(recipient, TABLET);
        given(sender.send(any(), any(), any(), any())).willReturn(new FcmSendResult(0, List.of(
                new FcmSendFailure(PHONE, "UNREGISTERED", "NOT_FOUND", 404, null),
                new FcmSendFailure(TABLET, "UNAVAILABLE", "UNAVAILABLE", 503, null))));
        Instant beforeJoin = Instant.now();

        clubService.join(joiner.getId(), JOIN_CODE);

        var task = ArgumentCaptor.forClass(FcmRetryTask.class);
        verify(retryService).schedule(task.capture());
        assertThat(task.getValue()).extracting(FcmRetryTask::notificationType, FcmRetryTask::sourceId)
                .containsExactly(NotificationType.CLUB_JOIN, club.getId());
        assertThat(task.getValue().createdAt()).isBetween(beforeJoin, task.getValue().observedAt());
        assertThat(task.getValue().registrationIds()).containsExactly(tablet.getId());
        assertThat(registrationRepository.findAll()).extracting(NotificationRegistration::getId)
                .containsExactly(tablet.getId());
    }

    @Test
    @DisplayName("호출 전체의 접수 불명확은 원래 대상별로 만들고 한 번의 추가 시도만 허용해요")
    void retryUncertainCallOnce() throws FirebaseMessagingException {
        register(recipient, TABLET);
        var exception = org.mockito.Mockito.mock(FirebaseMessagingException.class);
        given(exception.getErrorCode()).willReturn(ErrorCode.DEADLINE_EXCEEDED);
        given(sender.send(any(), any(), any(), any())).willThrow(exception);

        clubService.join(joiner.getId(), JOIN_CODE);

        var tasks = ArgumentCaptor.forClass(FcmRetryTask.class);
        verify(retryService, times(2)).schedule(tasks.capture());
        assertThat(tasks.getAllValues()).allMatch(task -> task.notificationType() == NotificationType.CLUB_JOIN);
        assertThat(tasks.getAllValues()).extracting(FcmRetryTask::fid).containsExactlyInAnyOrder(PHONE, TABLET);
        FcmRetryTask first = tasks.getAllValues().getFirst();
        assertThat(first.afterFailure(first.failure(), first.observedAt().plusSeconds(60), first.registrationIds())
                .orElseThrow().stopReason(0)).contains("UNCERTAIN_RETRY_LIMIT_REACHED");
    }

    private NotificationRegistration register(Member member, String fid) {
        return registrations.register(member.getId(), new NotificationRegistrationRequest(fid));
    }

    private Map<String, String> data() {
        return Map.of("type", "CLUB_JOIN", "clubId", club.getId().toString(), "memberId", joiner.getId().toString());
    }
}
