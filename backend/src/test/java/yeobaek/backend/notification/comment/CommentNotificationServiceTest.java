package yeobaek.backend.notification.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmPushSender;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;
import yeobaek.backend.support.LogCapture;

class CommentNotificationServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-19T00:00:00Z");

    private static final String FID = "c" + "a".repeat(21);
    private static final String TITLE = "새로운 댓글이 달렸어요!";
    private static final String BODY = "댓글을 남겼던 문장의 이야기를 확인해 보세요.";

    private final CommentNotificationRecipientReader reader = mock(CommentNotificationRecipientReader.class);
    private final FcmPushSender sender = mock(FcmPushSender.class);
    private final NotificationRegistrationService registrationService = mock(NotificationRegistrationService.class);
    private final FcmFailureHandler handler = mock(FcmFailureHandler.class);
    private final CommentNotificationService service = new CommentNotificationService(
            reader, sender, registrationService, handler);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("모임·공개방 모두 발송 전에 등록을 확인하고 SDK 결과를 실패 처리에 넘긴다")
    void handleResultAfterSnapshotAndSend(boolean club) throws FirebaseMessagingException {
        List<String> fids = List.of("c" + "a".repeat(21));
        var snapshot = new NotificationRegistrations(List.of());
        var result = new FcmSendResult(1, List.of());
        stubRecipients(club, fids);
        given(registrationService.snapshotForSend(fids)).willReturn(snapshot);
        given(sender.send(fids, "새로운 댓글이 달렸어요!", "댓글을 남겼던 문장의 이야기를 확인해 보세요.", data(club)))
                .willReturn(result);

        assertThat(notify(club)).isEmpty();

        var order = inOrder(registrationService, sender, handler);
        order.verify(registrationService).snapshotForSend(fids);
        order.verify(sender).send(fids, "새로운 댓글이 달렸어요!", "댓글을 남겼던 문장의 이야기를 확인해 보세요.", data(club));
        order.verify(handler).handle(result, snapshot);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("수신 대상이 없으면 스냅샷 조회·발송·실패 처리를 하지 않는다")
    void skipEmptyRecipients(boolean club) throws FirebaseMessagingException {
        stubRecipients(club, List.of());

        assertThat(notify(club)).isEmpty();

        verifyNoInteractions(registrationService, sender, handler);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("부분 실패 중 재시도 후보만 원래 등록 ID와 메시지를 담아 반환하고 추가 발송하지 않는다")
    void createOnlyRetryableTasks(boolean club) throws FirebaseMessagingException {
        String unregisteredFid = "d" + "a".repeat(21);
        String invalidFid = "e" + "a".repeat(21);
        List<String> fids = List.of(FID, unregisteredFid, invalidFid, "f" + "a".repeat(21));
        var failure = new FcmSendFailure(FID, "UNAVAILABLE", "UNAVAILABLE", 503, "120");
        var snapshot = new NotificationRegistrations(List.of(
                registration(1L, FID), registration(2L, FID),
                registration(3L, unregisteredFid), registration(4L, invalidFid)));
        var result = new FcmSendResult(1, List.of(failure,
                new FcmSendFailure(unregisteredFid, "UNREGISTERED", "NOT_FOUND", 404, null),
                new FcmSendFailure(invalidFid, "INVALID_ARGUMENT", "INVALID_ARGUMENT", 400, null)));
        stubRecipients(club, fids);
        given(registrationService.snapshotForSend(fids)).willReturn(snapshot);
        given(sender.send(fids, TITLE, BODY, data(club))).willReturn(result);

        List<FcmRetryTask> tasks = notify(club);

        assertThat(tasks).extracting(FcmRetryTask::commentId, FcmRetryTask::registrationIds,
                        FcmRetryTask::title, FcmRetryTask::body, FcmRetryTask::data, FcmRetryTask::failure)
                .containsExactly(tuple(11L, List.of(1L, 2L), TITLE, BODY, data(club), failure));
        verify(handler).handle(result, snapshot);
        verify(sender).send(fids, TITLE, BODY, data(club));
        verifyNoMoreInteractions(sender);
        assertThatThrownBy(tasks::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("접수 불명확·할당량 초과가 함께 있으면 대상별 작업에 원래 실패 메타데이터를 유지한다")
    void preserveMultipleRetryableFailures() throws FirebaseMessagingException {
        String quotaFid = "d" + "a".repeat(21);
        var failure = new FcmSendFailure(FID, "UNKNOWN", "DEADLINE_EXCEEDED", null, null);
        var quotaFailure = new FcmSendFailure(quotaFid, "QUOTA_EXCEEDED", "RESOURCE_EXHAUSTED", 429, "120");
        List<String> fids = List.of(FID, quotaFid);
        stubRecipients(true, fids);
        given(registrationService.snapshotForSend(fids)).willReturn(
                new NotificationRegistrations(List.of(registration(1L, FID), registration(2L, quotaFid))));
        given(sender.send(fids, TITLE, BODY, data(true))).willReturn(new FcmSendResult(0, List.of(failure, quotaFailure)));

        List<FcmRetryTask> tasks = notify(true);

        assertThat(tasks).extracting(FcmRetryTask::registrationIds, FcmRetryTask::failure)
                .containsExactly(tuple(List.of(1L), failure), tuple(List.of(2L), quotaFailure));
    }

    @Test
    @DisplayName("발송 당시 등록이 없는 후보는 작업을 만들지 않고 민감 정보 없이 경고한다")
    void skipMissingSnapshot() throws FirebaseMessagingException {
        var failure = new FcmSendFailure(FID, "INTERNAL", "INTERNAL", 500, null);
        stubSend(failure, new NotificationRegistrations(List.of()));

        try (var logs = new LogCapture(CommentNotificationService.class.getName())) {
            assertThat(notify(true)).isEmpty();

            var event = logs.event("notification.comment.createRetryTasks", "rejected");
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(logs.field(event, "reason")).isEqualTo("registrationSnapshotMissing");
            assertThat(logs.field(event, "commentId")).isEqualTo(11L);
            assertThat(logs.structuredText()).doesNotContain(FID, TITLE, BODY);
        }
    }

    @Test
    @DisplayName("재시도하지 않는 실패는 기존 실패 처리에 넘기고 작업은 반환하지 않는다")
    void skipPermanentFailure() throws FirebaseMessagingException {
        var failure = new FcmSendFailure(FID, "INVALID_ARGUMENT", "INVALID_ARGUMENT", 400, null);
        stubSend(failure, new NotificationRegistrations(List.of(registration(1L, FID))));

        assertThat(notify(true)).isEmpty();
    }

    @Test
    @DisplayName("SDK 호출 자체가 실패하면 개별 응답을 만들지 않고 기존 호출 실패 경로로 전달한다")
    void propagateWholeCallFailure() throws FirebaseMessagingException {
        List<String> fids = List.of(FID);
        var exception = mock(FirebaseMessagingException.class);
        given(exception.getErrorCode()).willReturn(ErrorCode.UNKNOWN);
        stubRecipients(true, fids);
        given(registrationService.snapshotForSend(fids)).willReturn(new NotificationRegistrations(List.of()));
        given(sender.send(fids, TITLE, BODY, data(true))).willThrow(exception);

        assertThatThrownBy(() -> notify(true)).isSameAs(exception);
        verifyNoInteractions(handler);
        verify(sender).send(fids, TITLE, BODY, data(true));
        verifyNoMoreInteractions(sender);
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
        stubRecipients(club, fids);
        given(registrationService.snapshotForSend(fids)).willReturn(new NotificationRegistrations(List.of(
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
        stubRecipients(true, List.of(FID));
        given(registrationService.snapshotForSend(List.of(FID)))
                .willReturn(new NotificationRegistrations(List.of(registration(1L, FID))));
        given(sender.send(List.of(FID), TITLE, BODY, data(true))).willThrow(exception);

        FcmRetryTask task = service.notifyReadersInClub(11L, 5L, 13L, 7L, 3, Instant.now()).getFirst();

        assertThat(task.isCallFailure()).isTrue();
        assertThat(task.failure().isReceiptUncertain()).isFalse();
        assertThat(task.failure().httpStatus()).isEqualTo(httpStatus);
        assertThat(task.nextRetryAt(0)).contains(task.observedAt().plusSeconds(300));
        verify(handler).handleCallFailure(11L, exception);
    }

    private void stubSend(FcmSendFailure failure, NotificationRegistrations snapshot)
            throws FirebaseMessagingException {
        List<String> fids = List.of(FID);
        stubRecipients(true, fids);
        given(registrationService.snapshotForSend(fids)).willReturn(snapshot);
        given(sender.send(fids, TITLE, BODY, data(true))).willReturn(new FcmSendResult(0, List.of(failure)));
    }

    private NotificationRegistration registration(Long id, String fid) {
        return new NotificationRegistration(id, mock(Member.class), new FirebaseInstallationId(fid));
    }

    private void stubRecipients(boolean club, List<String> fids) {
        if (club) {
            given(reader.findReadersInClub(5L, 13L, 7L)).willReturn(fids);
        } else {
            given(reader.findPreviousCommentersInPublicRoom(8L, 13L, 7L)).willReturn(fids);
        }
    }

    private Map<String, String> data(boolean club) {
        return Map.of("type", club ? "CLUB_COMMENT" : "PUBLIC_ROOM_COMMENT",
                club ? "clubId" : "publicRoomId", club ? "5" : "8",
                "passageSequence", "3", "sentenceId", "13");
    }

    private List<FcmRetryTask> notify(boolean club) throws FirebaseMessagingException {
        if (club) {
            return service.notifyReadersInClub(11L, 5L, 13L, 7L, 3, CREATED_AT);
        } else {
            return service.notifyPreviousCommentersInPublicRoom(11L, 8L, 13L, 7L, 3, CREATED_AT);
        }
    }
}
