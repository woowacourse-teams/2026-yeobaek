package yeobaek.backend.notification.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ch.qos.logback.classic.Level;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.ErrorCode;
import com.google.firebase.IncomingHttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.PlatformTransactionManager;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.repository.SentenceRepository;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.member.repository.MemberRepository;
import yeobaek.backend.comment.event.ClubSentenceCommentCreatedEvent;
import yeobaek.backend.comment.event.PublicRoomSentenceCommentCreatedEvent;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.FcmRetryService;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.support.LogCapture;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;

class CommentNotificationServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-19T00:00:00Z");

    private static final String FID = "c" + "a".repeat(21);
    private static final String TITLE = "새로운 댓글이 달렸어요!";
    private static final String BODY = "댓글을 남겼던 문장의 이야기를 확인해 보세요.";

    private final NotificationRegistrationService registrations = mock(NotificationRegistrationService.class);
    private final CommentNotificationRecipientQueryRepository query = mock(CommentNotificationRecipientQueryRepository.class);
    private final ClubMemberRepository clubMembers = mock(ClubMemberRepository.class);
    private final SentenceRepository sentences = mock(SentenceRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final FcmPushSender sender = mock(FcmPushSender.class);
    private final FcmFailureHandler handler = mock(FcmFailureHandler.class);
    private final FcmRetryService retryService = mock(FcmRetryService.class);
    private final List<FcmRetryTask> scheduledTasks = new ArrayList<>();
    private final CommentNotificationService service = new CommentNotificationService(
            sender, handler, retryService, registrations, query, clubMembers, sentences, members,
            mock(PlatformTransactionManager.class));

    @BeforeEach
    void captureScheduledTasks() {
        doAnswer(invocation -> {
            scheduledTasks.add(invocation.getArgument(0));
            return null;
        }).when(retryService).schedule(any(FcmRetryTask.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("모임·공개방 모두 발송 전에 등록을 확인하고 SDK 결과를 실패 처리에 넘긴다")
    void handleResultAfterSnapshotAndSend(boolean club) throws FirebaseMessagingException {
        List<String> fids = List.of("c" + "a".repeat(21));
        var snapshot = new NotificationRegistrations(List.of(registration(1L, FID)));
        var result = new FcmSendResult(1, List.of());
        stubRecipients(club, snapshot);
        given(sender.send(fids, "새로운 댓글이 달렸어요!", "댓글을 남겼던 문장의 이야기를 확인해 보세요.", data(club)))
                .willReturn(result);

        assertThat(notify(club)).isEmpty();

        var order = inOrder(registrations, query, sender, handler);
        if (club) {
            order.verify(registrations).findByMembers(any());
        } else {
            order.verify(query).findRegistrationsForPublicRoomComment(8L, 13L, 7L);
        }
        order.verify(sender).send(fids, "새로운 댓글이 달렸어요!", "댓글을 남겼던 문장의 이야기를 확인해 보세요.", data(club));
        order.verify(handler).handle(eq(result), any(NotificationRegistrations.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("수신 등록이 없으면 발송·실패 처리를 하지 않는다")
    void skipEmptyRecipients(boolean club) throws FirebaseMessagingException {
        stubRecipients(club, new NotificationRegistrations(List.of()));

        assertThat(notify(club)).isEmpty();

        verifyNoInteractions(sender, handler);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("부분 실패 중 재시도 후보만 원래 등록 ID와 메시지를 담아 예약하고 즉시 추가 발송하지 않는다")
    void createOnlyRetryableTasks(boolean club) throws FirebaseMessagingException {
        String unregisteredFid = "d" + "a".repeat(21);
        String invalidFid = "e" + "a".repeat(21);
        List<String> fids = List.of(FID, unregisteredFid, invalidFid, "f" + "a".repeat(21));
        var failure = new FcmSendFailure(FID, "UNAVAILABLE", "UNAVAILABLE", 503, "120");
        var snapshot = new NotificationRegistrations(List.of(
                registration(1L, FID), registration(2L, FID),
                registration(3L, unregisteredFid), registration(4L, invalidFid), registration(5L, fids.get(3))));
        var result = new FcmSendResult(1, List.of(failure,
                new FcmSendFailure(unregisteredFid, "UNREGISTERED", "NOT_FOUND", 404, null),
                new FcmSendFailure(invalidFid, "INVALID_ARGUMENT", "INVALID_ARGUMENT", 400, null)));
        stubRecipients(club, snapshot);
        given(sender.send(fids, TITLE, BODY, data(club))).willReturn(result);

        List<FcmRetryTask> tasks = notify(club);

        assertThat(tasks).extracting(FcmRetryTask::commentId, FcmRetryTask::registrationIds,
                        FcmRetryTask::title, FcmRetryTask::body, FcmRetryTask::data, FcmRetryTask::failure)
                .containsExactly(tuple(11L, List.of(1L, 2L), TITLE, BODY, data(club), failure));
        verify(handler).handle(eq(result), any(NotificationRegistrations.class));
        verify(sender).send(fids, TITLE, BODY, data(club));
        verifyNoMoreInteractions(sender);
    }

    @Test
    @DisplayName("접수 불명확·할당량 초과가 함께 있으면 대상별 작업에 원래 실패 메타데이터를 유지한다")
    void preserveMultipleRetryableFailures() throws FirebaseMessagingException {
        String quotaFid = "d" + "a".repeat(21);
        var failure = new FcmSendFailure(FID, "UNKNOWN", "DEADLINE_EXCEEDED", null, null);
        var quotaFailure = new FcmSendFailure(quotaFid, "QUOTA_EXCEEDED", "RESOURCE_EXHAUSTED", 429, "120");
        List<String> fids = List.of(FID, quotaFid);
        stubRecipients(true,
                new NotificationRegistrations(List.of(registration(1L, FID), registration(2L, quotaFid))));
        given(sender.send(fids, TITLE, BODY, data(true))).willReturn(new FcmSendResult(0, List.of(failure, quotaFailure)));

        List<FcmRetryTask> tasks = notify(true);

        assertThat(tasks).extracting(FcmRetryTask::registrationIds, FcmRetryTask::failure)
                .containsExactly(tuple(List.of(1L), failure), tuple(List.of(2L), quotaFailure));
    }

    @Test
    @DisplayName("재시도하지 않는 실패는 기존 실패 처리에 넘기고 작업은 예약하지 않는다")
    void skipPermanentFailure() throws FirebaseMessagingException {
        var failure = new FcmSendFailure(FID, "INVALID_ARGUMENT", "INVALID_ARGUMENT", 400, null);
        stubSend(failure, new NotificationRegistrations(List.of(registration(1L, FID))));

        assertThat(notify(true)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("재시도하지 않는 SDK 호출 실패는 기존 실패 처리에 넘기고 후보 수를 기록하지 않는다")
    void handlePermanentWholeCallFailure(boolean club) throws FirebaseMessagingException {
        var exception = mock(FirebaseMessagingException.class);
        given(exception.getErrorCode()).willReturn(ErrorCode.UNKNOWN);
        stubRecipients(club, new NotificationRegistrations(List.of(registration(1L, FID))));
        given(sender.send(List.of(FID), TITLE, BODY, data(club))).willThrow(exception);

        try (var logs = new LogCapture(CommentNotificationService.class.getName())) {
            notify(club);

            assertThat(logs.structuredText()).doesNotContain("retryTaskCount");
        }
        verify(handler).handleCallFailure(11L, exception);
        verifyNoInteractions(retryService);
        verify(sender).send(List.of(FID), TITLE, BODY, data(club));
        verifyNoMoreInteractions(sender);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("모임·공개방 모두 재시도 후보를 예약하고 민감 정보 없이 후보 수를 기록한다")
    void recordCreatedTaskCount(boolean club) throws FirebaseMessagingException {
        var failure = new FcmSendFailure(FID, "INTERNAL", "INTERNAL", 500, null);
        stubRecipients(club, new NotificationRegistrations(List.of(registration(1L, FID))));
        given(sender.send(List.of(FID), TITLE, BODY, data(club)))
                .willReturn(new FcmSendResult(0, List.of(failure)));

        try (var logs = new LogCapture(CommentNotificationService.class.getName())) {
            List<FcmRetryTask> tasks = notify(club);

            var event = logs.event("notification.comment.logRetryTasks", "success");
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(logs.field(event, "commentId")).isEqualTo(11L);
            assertThat(logs.field(event, "retryTaskCount")).isEqualTo(1);
            assertThat(logs.structuredText()).doesNotContain(FID, TITLE, BODY);
            verify(retryService).schedule(tasks.getFirst());
        }
    }

    @Test
    @DisplayName("최초 실패 작업은 댓글 생성 시각과 결과 확인 시각을 보관하고 추가 발송 이력은 0회다")
    void initializeRetryHistoryBeforeCleanup() throws FirebaseMessagingException {
        var failure = new FcmSendFailure(FID, "INTERNAL", "INTERNAL", 500, null);
        var snapshot = new NotificationRegistrations(List.of(registration(1L, FID)));
        var cleanupStartedAt = new AtomicReference<Instant>();
        stubSend(failure, snapshot);
        doAnswer(invocation -> {
            cleanupStartedAt.set(Instant.now());
            return null;
        }).when(handler).handle(new FcmSendResult(0, List.of(failure)), snapshot);
        Instant beforeSend = Instant.now();

        FcmRetryTask task = notify(true).getFirst();

        assertThat(task.commentCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(task.observedAt()).isBetween(beforeSend, cleanupStartedAt.get());
        assertThat(task.retriesPerformed()).isZero();
        assertThat(task.uncertainRetryUsed()).isFalse();
        verify(sender).send(List.of(FID), TITLE, BODY, data(true));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("응답 없는 최초 호출 실패는 개별 실패 응답을 만들지 않고 원래 등록에만 추가 시도 대상을 연결한다")
    void preserveCallScopeForInitialUncertainty(boolean club) throws FirebaseMessagingException {
        String second = "d" + "a".repeat(21);
        List<String> fids = List.of(FID, second);
        var exception = mock(FirebaseMessagingException.class);
        given(exception.getErrorCode()).willReturn(ErrorCode.DEADLINE_EXCEEDED);
        stubRecipients(club, new NotificationRegistrations(List.of(
                registration(1L, FID), registration(2L, second))));
        given(sender.send(fids, TITLE, BODY, data(club))).willThrow(exception);

        List<FcmRetryTask> tasks = notify(club);

        assertThat(tasks).extracting(FcmRetryTask::fid, task -> task.failure().fid(),
                        FcmRetryTask::isCallFailure, FcmRetryTask::registrationIds, FcmRetryTask::data)
                .containsExactly(tuple(FID, null, true, List.of(1L), data(club)),
                        tuple(second, null, true, List.of(2L), data(club)));
        verify(handler).handleCallFailure(11L, exception);
    }

    @ParameterizedTest
    @CsvSource({"500,INTERNAL", "429,RESOURCE_EXHAUSTED", "503,UNAVAILABLE"})
    @DisplayName("호출 전체 일시 오류도 원래 오류·Retry-After를 유지하며 접수 불명확과 구분한다")
    void retainRetryableCallError(int httpStatus, String sdkCode) throws FirebaseMessagingException {
        var exception = mock(FirebaseMessagingException.class);
        var response = mock(IncomingHttpResponse.class);
        given(exception.getErrorCode()).willReturn(ErrorCode.valueOf(sdkCode));
        given(exception.getHttpResponse()).willReturn(response);
        given(response.getStatusCode()).willReturn(httpStatus);
        given(response.getHeaders()).willReturn(Map.of("Retry-After", "300"));
        stubRecipients(true, new NotificationRegistrations(List.of(registration(1L, FID))));
        given(sender.send(List.of(FID), TITLE, BODY, data(true))).willThrow(exception);

        service.notifyClub(new ClubSentenceCommentCreatedEvent(11L, 5L, 13L, 7L, 3, Instant.now()));
        FcmRetryTask task = scheduledTasks.getFirst();

        assertThat(task.isCallFailure()).isTrue();
        assertThat(task.failure().isReceiptUncertain()).isFalse();
        assertThat(task.failure().httpStatus()).isEqualTo(httpStatus);
        assertThat(task.nextRetryAt(0)).contains(task.observedAt().plusSeconds(300));
        verify(handler).handleCallFailure(11L, exception);
    }

    private void stubSend(FcmSendFailure failure, NotificationRegistrations snapshot)
            throws FirebaseMessagingException {
        List<String> fids = List.of(FID);
        stubRecipients(true, snapshot);
        given(sender.send(fids, TITLE, BODY, data(true))).willReturn(new FcmSendResult(0, List.of(failure)));
    }

    private NotificationRegistration registration(Long id, String fid) {
        return new NotificationRegistration(id, mock(Member.class), new FirebaseInstallationId(fid));
    }

    private void stubRecipients(boolean club, NotificationRegistrations snapshot) {
        if (club) {
            var sentence = mock(Sentence.class);
            var passage = mock(Passage.class);
            given(passage.getSequence()).willReturn(new ContentSequence(1));
            given(sentence.getPassage()).willReturn(passage);
            given(sentences.getReferenceById(13L)).willReturn(sentence);
            given(members.getReferenceById(7L)).willReturn(mock(Member.class));
            given(clubMembers.findNotificationCandidatesWithReadingProgress(5L, 7L)).willReturn(List.of());
            given(registrations.findByMembers(any())).willReturn(snapshot);
        } else {
            List<NotificationRegistration> selected = snapshot.firebaseInstallationIds().stream()
                    .flatMap(fid -> snapshot.idsFor(new FirebaseInstallationId(fid)).stream()
                            .map(id -> registration(id, fid))).toList();
            given(query.findRegistrationsForPublicRoomComment(8L, 13L, 7L)).willReturn(selected);
        }
    }

    private Map<String, String> data(boolean club) {
        return Map.of("type", club ? "CLUB_COMMENT" : "PUBLIC_ROOM_COMMENT",
                club ? "clubId" : "publicRoomId", club ? "5" : "8",
                "passageSequence", "3", "sentenceId", "13");
    }

    private List<FcmRetryTask> notify(boolean club) {
        if (club) {
            service.notifyClub(new ClubSentenceCommentCreatedEvent(11L, 5L, 13L, 7L, 3, CREATED_AT));
        } else {
            service.notifyPublicRoom(new PublicRoomSentenceCommentCreatedEvent(11L, 8L, 13L, 7L, 3, CREATED_AT));
        }
        return List.copyOf(scheduledTasks);
    }
}
