package yeobaek.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessagingException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.scheduling.TaskScheduler;
import yeobaek.backend.member.domain.Member;
import yeobaek.backend.comment.domain.ClubCommentCreated;
import yeobaek.backend.notification.comment.CommentNotificationRecipientReader;
import yeobaek.backend.notification.comment.CommentNotificationService;
import yeobaek.backend.notification.comment.CommentNotificationWorker;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.notification.domain.FcmSendResult;
import yeobaek.backend.notification.registration.domain.FirebaseInstallationId;
import yeobaek.backend.notification.registration.domain.NotificationRegistration;
import yeobaek.backend.notification.registration.domain.NotificationRegistrations;
import yeobaek.backend.notification.registration.service.NotificationRegistrationService;
import yeobaek.backend.support.LogCapture;

class FcmRetryProcessorTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-19T00:00:00Z");
    private static final String FID = "c" + "a".repeat(21);
    private static final String TITLE = "민감한 제목";
    private static final String BODY = "민감한 본문";
    private final TaskScheduler scheduler = mock(TaskScheduler.class);
    private final Clock clock = mock(Clock.class);
    private final FcmPushSender sender = mock(FcmPushSender.class);
    private final NotificationRegistrationService registrations = mock(NotificationRegistrationService.class);
    private final FcmFailureHandler handler = mock(FcmFailureHandler.class);
    private final AtomicReference<Instant> now = new AtomicReference<>(CREATED_AT);
    private final Deque<Runnable> timers = new ArrayDeque<>();
    private final Deque<Instant> scheduledTimes = new ArrayDeque<>();
    private final Deque<Runnable> workers = new ArrayDeque<>();
    private FcmRetryProcessor processor;

    @BeforeEach
    void setUp() throws FirebaseMessagingException {
        given(clock.instant()).willAnswer(invocation -> now.get());
        captureSchedules();
        given(registrations.snapshotForRetry(anyList(), any(FirebaseInstallationId.class)))
                .willAnswer(invocation -> snapshot(invocation.getArgument(0), invocation.getArgument(1)));
        given(sender.send(anyList(), anyString(), anyString())).willReturn(new FcmSendResult(1, List.of()));
        processor = newProcessor(workers::addLast, handler, 10);
    }

    @Test
    @DisplayName("예약기는 발송하지 않고 실행기에 넘기며 성공하면 해당 실패 FID의 작업을 끝낸다")
    void dispatchOnlyFailedTargetAndStopOnSuccess() throws FirebaseMessagingException {
        processor.schedule(task(internal(null), 0, false));
        verifyNoInteractions(sender, registrations);

        fireTimer();
        assertThat(workers).hasSize(1);
        verifyNoInteractions(sender, registrations);
        workers.removeFirst().run();

        verify(sender).send(List.of(FID), TITLE, BODY);
        assertThat(timers).isEmpty();
    }

    @Test
    @DisplayName("Retry-After보다 짧게 예약하지 않고 지연 비율까지 반영한다")
    void respectServerWait() {
        processor.schedule(task(internal("120"), 0, false));

        assertThat(scheduledTimes.getFirst()).isBetween(CREATED_AT.plusSeconds(120), CREATED_AT.plusSeconds(132));
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("추가 발송이 429로 실패하면 새 Retry-After를 다음 예약에 반영한다")
    void respectNewQuotaResponseOnNextAttempt() throws FirebaseMessagingException {
        var quota = new FcmSendFailure(FID, "QUOTA_EXCEEDED", "RESOURCE_EXHAUSTED", 429, "300");
        given(sender.send(List.of(FID), TITLE, BODY)).willReturn(
                new FcmSendResult(0, List.of(quota)), new FcmSendResult(1, List.of()));
        processor.schedule(task(internal(null), 0, false));
        completeNext();

        assertThat(scheduledTimes.getFirst()).isBetween(now.get().plusSeconds(300), now.get().plusSeconds(324));
        completeNext();

        assertThat(timers).isEmpty();
        verify(sender, times(2)).send(List.of(FID), TITLE, BODY);
    }

    @Test
    @DisplayName("중복 등록 중 남은 원래 등록 ID만 후속 작업에 이어서 확인한다")
    void shrinkRegistrationSnapshotForNextAttempt() throws FirebaseMessagingException {
        var fid = new FirebaseInstallationId(FID);
        given(registrations.snapshotForRetry(List.of(1L, 2L), fid)).willReturn(snapshot(List.of(2L), fid));
        given(sender.send(List.of(FID), TITLE, BODY)).willReturn(
                new FcmSendResult(0, List.of(internal(null))), new FcmSendResult(1, List.of()));
        processor.schedule(task(internal(null), 0, false));

        completeNext();
        completeNext();

        verify(registrations).snapshotForRetry(List.of(1L, 2L), fid);
        verify(registrations).snapshotForRetry(List.of(2L), fid);
        assertThat(timers).isEmpty();
    }

    @Test
    @DisplayName("일시 실패가 계속되면 최초 발송 외 정확히 다섯 번 시도하고 종료한다")
    void retryAtMostFiveTimes() throws FirebaseMessagingException {
        given(sender.send(List.of(FID), TITLE, BODY)).willReturn(new FcmSendResult(0, List.of(internal(null))));
        try (var logs = new LogCapture(FcmRetryProcessor.class.getName())) {
            processor.schedule(task(internal(null), 0, false));
            for (int attempt = 0; attempt < 5; attempt++) {
                completeNext();
            }

            var event = logs.event("notification.retry.logStopped", "rejected");
            assertThat(logs.field(event, "stopReason")).isEqualTo("RETRY_LIMIT_REACHED");
            assertThat(logs.field(event, "retriesPerformed")).isEqualTo(5);
            assertThat(timers).isEmpty();
            assertThat(workers).isEmpty();
        }
        verify(sender, times(5)).send(List.of(FID), TITLE, BODY);
    }

    @Test
    @DisplayName("접수 불명확에 따른 추가 발송이 다시 불명확하면 한 번으로 종료한다")
    void retryUncertainReceiptOnlyOnce() throws FirebaseMessagingException {
        var uncertain = uncertain();
        given(sender.send(List.of(FID), TITLE, BODY)).willReturn(new FcmSendResult(0, List.of(uncertain)));
        try (var logs = new LogCapture(FcmRetryProcessor.class.getName())) {
            processor.schedule(task(uncertain, 0, false));
            completeNext();

            var event = logs.event("notification.retry.logStopped", "rejected");
            assertThat(logs.field(event, "stopReason")).isEqualTo("UNCERTAIN_RETRY_LIMIT_REACHED");
            assertThat(logs.field(event, "retriesPerformed")).isEqualTo(1);
            assertThat(logs.field(event, "uncertainRetryUsed")).isEqualTo(true);
            assertThat(timers).isEmpty();
        }
        verify(sender).send(List.of(FID), TITLE, BODY);
    }

    @Test
    @DisplayName("오류 종류가 바뀌어도 이미 사용한 접수 불명확 추가 발송 이력을 유지한다")
    void retainUncertaintyHistoryAcrossFailureTypes() throws FirebaseMessagingException {
        given(sender.send(List.of(FID), TITLE, BODY)).willReturn(
                new FcmSendResult(0, List.of(uncertain())),
                new FcmSendResult(0, List.of(internal(null))),
                new FcmSendResult(0, List.of(uncertain())));
        try (var logs = new LogCapture(FcmRetryProcessor.class.getName())) {
            processor.schedule(task(internal(null), 0, false));
            completeNext();
            completeNext();
            completeNext();

            var event = logs.event("notification.retry.logStopped", "rejected");
            assertThat(logs.field(event, "stopReason")).isEqualTo("UNCERTAIN_RETRY_LIMIT_REACHED");
            assertThat(logs.field(event, "retriesPerformed")).isEqualTo(3);
            assertThat(logs.field(event, "uncertainRetryUsed")).isEqualTo(true);
            assertThat(timers).isEmpty();
        }
        verify(sender, times(3)).send(List.of(FID), TITLE, BODY);
    }

    @Test
    @DisplayName("추가 단일 대상 호출의 타임아웃도 접수 불명확 이력을 공유해 한 번만 더 시도한다")
    void classifyRetryCallTimeoutWithoutBatchResponse() throws FirebaseMessagingException {
        var exception = mock(FirebaseMessagingException.class);
        given(exception.getErrorCode()).willReturn(ErrorCode.DEADLINE_EXCEEDED);
        given(sender.send(List.of(FID), TITLE, BODY)).willThrow(exception);
        processor.schedule(task(internal(null), 0, false));

        completeNext();
        completeNext();

        verify(sender, times(2)).send(List.of(FID), TITLE, BODY);
        verify(handler, times(2)).handleCallFailure(11L, exception);
        assertThat(timers).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "INVALID_ARGUMENT,INVALID_ARGUMENT,400",
            "SENDER_ID_MISMATCH,PERMISSION_DENIED,403",
            "THIRD_PARTY_AUTH_ERROR,UNAUTHENTICATED,401",
            "UNKNOWN,UNAUTHENTICATED,401",
            "UNKNOWN,PERMISSION_DENIED,403",
            "UNKNOWN,UNKNOWN,502"
    })
    @DisplayName("요청·인증·미확인 오류는 실패 처리 후 재예약하지 않고 실제 시도 횟수를 남긴다")
    void stopPermanentFailure(String detail, String sdk, int status) throws FirebaseMessagingException {
        var result = new FcmSendResult(0, List.of(new FcmSendFailure(FID, detail, sdk, status, null)));
        given(sender.send(List.of(FID), TITLE, BODY)).willReturn(result);
        try (var logs = new LogCapture(FcmRetryProcessor.class.getName())) {
            processor.schedule(task(internal(null), 0, false));
            completeNext();

            assertThat(timers).isEmpty();
            assertThat(logs.field(logs.event("notification.retry.logStopped", "rejected"), "retriesPerformed"))
                    .isEqualTo(1);
        }
        verify(handler).handle(any(FcmSendResult.class), any(NotificationRegistrations.class));
    }

    @Test
    @DisplayName("UNREGISTERED는 이번 발송 직전에 남아 있던 원래 등록만 삭제하고 종료한다")
    void removeUnregisteredWithoutSchedulingAgain() throws FirebaseMessagingException {
        var fid = new FirebaseInstallationId(FID);
        var remaining = snapshot(List.of(1L), fid);
        given(registrations.snapshotForRetry(List.of(1L, 2L), fid)).willReturn(remaining);
        given(sender.send(List.of(FID), TITLE, BODY)).willReturn(new FcmSendResult(0,
                List.of(new FcmSendFailure(FID, "UNREGISTERED", "NOT_FOUND", 404, null))));
        processor = newProcessor(workers::addLast, new FcmFailureHandler(registrations), 10);
        processor.schedule(task(internal(null), 0, false));

        completeNext();

        verify(registrations).deleteUnregistered(remaining, fid);
        assertThat(timers).isEmpty();
        verify(sender).send(List.of(FID), TITLE, BODY);
    }

    @Test
    @DisplayName("원래 등록이 삭제되거나 FID가 바뀌었다면 새 등록으로 대체하지 않고 종료한다")
    void stopWhenOriginalRegistrationDisappears() {
        given(registrations.snapshotForRetry(anyList(), any())).willReturn(new NotificationRegistrations(List.of()));
        processor.schedule(task(internal(null), 0, false));

        completeNext();

        verify(registrations).snapshotForRetry(List.of(1L, 2L), new FirebaseInstallationId(FID));
        verifyNoInteractions(sender);
        assertThat(timers).isEmpty();
    }

    @Test
    @DisplayName("실행기 대기 중 30분이 지나면 DB 조회와 발송 없이 종료한다")
    void stopExpiredWhileQueued() {
        processor.schedule(task(internal(null), 0, false));
        fireTimer();
        now.set(CREATED_AT.plusSeconds(1800));

        workers.removeFirst().run();

        verifyNoInteractions(registrations, sender);
        assertThat(timers).isEmpty();
    }

    @Test
    @DisplayName("등록 조회가 지연돼 30분이 지났다면 FCM을 호출하기 전에 다시 만료를 확인한다")
    void recheckExpiryAfterDatabaseQuery() {
        given(registrations.snapshotForRetry(anyList(), any())).willAnswer(invocation -> {
            now.set(CREATED_AT.plusSeconds(1800));
            return snapshot(List.of(1L), new FirebaseInstallationId(FID));
        });
        processor.schedule(task(internal(null), 0, false));

        completeNext();

        verifyNoInteractions(sender);
        assertThat(timers).isEmpty();
    }

    @Test
    @DisplayName("서버 대기 요구가 유효기간을 넘으면 예약도 발송도 하지 않는다")
    void stopWhenRetryAfterExceedsValidity() {
        try (var logs = new LogCapture(FcmRetryProcessor.class.getName())) {
            processor.schedule(task(internal("1800"), 0, false));

            assertThat(timers).isEmpty();
            assertThat(logs.field(logs.event("notification.retry.logStopped", "rejected"), "stopReason"))
                    .isEqualTo("NEXT_RETRY_AT_OR_AFTER_EXPIRY");
        }
        verifyNoInteractions(sender, registrations);
    }

    @Test
    @DisplayName("예약 수 제한을 넘는 작업은 버리고 타이머가 실행되면 예약 공간을 다시 사용할 수 있다")
    void boundPendingTimersAndReleaseOnDispatch() {
        processor = newProcessor(workers::addLast, handler, 1);
        try (var logs = new LogCapture(FcmRetryProcessor.class.getName())) {
            processor.schedule(task(internal(null), 0, false));
            processor.schedule(task(internal(null), 0, false));

            assertThat(timers).hasSize(1);
            assertThat(logs.field(logs.event("notification.retry.logRejected", "rejected"), "reason"))
                    .isEqualTo("pendingCapacityExceeded");
            fireTimer();
            processor.schedule(task(internal(null), 0, false));
            assertThat(timers).hasSize(1);
        }
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("스케줄러 제출 거절로 확보했던 예약 공간을 잃지 않는다")
    void releaseCapacityAfterSchedulerRejection() {
        processor = newProcessor(workers::addLast, handler, 1);
        given(scheduler.schedule(any(Runnable.class), any(Instant.class))).willThrow(new TaskRejectedException("closed"));
        processor.schedule(task(internal(null), 0, false));
        captureSchedules();

        processor.schedule(task(internal(null), 0, false));

        assertThat(timers).hasSize(1);
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("실행기가 거절하면 타이머 스레드에서 발송하지 않고 예약 공간도 반환한다")
    void dropOnExecutorRejectionWithoutCallerRuns() {
        processor = newProcessor(task -> { throw new TaskRejectedException("full"); }, handler, 1);
        try (var logs = new LogCapture(FcmRetryProcessor.class.getName())) {
            processor.schedule(task(internal(null), 0, false));
            fireTimer();
            processor.schedule(task(internal(null), 0, false));

            assertThat(logs.field(logs.event("notification.retry.logRejected", "rejected"), "reason"))
                    .isEqualTo("executorRejected");
            assertThat(timers).hasSize(1);
        }
        verifyNoInteractions(sender, registrations);
    }

    @Test
    @DisplayName("등록 조회 실패는 민감 정보를 기록하지 않고 종료한다")
    void stopOnDatabaseFailure() {
        given(registrations.snapshotForRetry(anyList(), any()))
                .willThrow(new DataAccessResourceFailureException("민감한 SQL " + FID));
        try (var logs = new LogCapture(FcmRetryProcessor.class.getName())) {
            processor.schedule(task(internal(null), 0, false));
            completeNext();

            assertThat(logs.field(logs.event("notification.retry.execute", "failure"), "exceptionType"))
                    .isEqualTo("DataAccessResourceFailureException");
            assertThat(logs.structuredText()).doesNotContain(FID, TITLE, BODY, "민감한 SQL");
            assertThat(timers).isEmpty();
        }
        verifyNoInteractions(sender);
    }

    @Test
    @DisplayName("예상하지 못한 오류를 관찰하고 후속 작업을 계속 처리한다")
    void observeUnexpectedFailureAndContinue() throws FirebaseMessagingException {
        given(registrations.snapshotForRetry(anyList(), any())).willThrow(new IllegalStateException(FID));
        try (var logs = new LogCapture(FcmRetryProcessor.class.getName())) {
            processor.schedule(task(internal(null), 0, false));
            completeNext();

            assertThat(logs.field(logs.event("notification.retry.submit", "failure"), "reason"))
                    .isEqualTo("unexpectedFailure");
            assertThat(logs.structuredText()).doesNotContain(FID);
        }
        doReturn(snapshot(List.of(1L), new FirebaseInstallationId(FID)))
                .when(registrations).snapshotForRetry(anyList(), any());
        processor.schedule(task(internal(null), 0, false));
        completeNext();
        verify(sender).send(List.of(FID), TITLE, BODY);
    }

    @Test
    @DisplayName("SDK 요청 검증 실패는 재예약하지 않는다")
    void stopInvalidRequest() throws FirebaseMessagingException {
        doThrow(new IllegalArgumentException(BODY)).when(sender).send(List.of(FID), TITLE, BODY);
        processor.schedule(task(internal(null), 0, false));

        completeNext();

        verify(handler).handleInvalidRequest(11L);
        assertThat(timers).isEmpty();
    }

    @Test
    @DisplayName("최초 부분 실패부터 연결하면 재시도 가능한 실패 FID만 한 번 추가 발송한다")
    void retryOnlyEligibleTargetFromInitialBatch() throws FirebaseMessagingException {
        now.set(Instant.now());
        String second = "d" + "a".repeat(21);
        String third = "e" + "a".repeat(21);
        List<String> fids = List.of(FID, second, third);
        String title = "새로운 댓글이 달렸어요!";
        String body = "댓글을 남겼던 문장의 이야기를 확인해 보세요.";
        var reader = mock(CommentNotificationRecipientReader.class);
        given(reader.findReadersInClub(5L, 13L, 7L)).willReturn(fids);
        given(registrations.snapshotForSend(fids)).willReturn(new NotificationRegistrations(List.of(
                registration(1L, FID), registration(2L, second), registration(3L, third))));
        given(sender.send(fids, title, body)).willReturn(new FcmSendResult(1, List.of(
                internal(null), new FcmSendFailure(second, "UNREGISTERED", "NOT_FOUND", 404, null))));
        var service = new CommentNotificationService(reader, sender, registrations, handler);
        var worker = new CommentNotificationWorker(service, handler, processor);

        worker.notifyClub(new ClubCommentCreated(11L, 5L, 13L, 7L, now.get()));
        assertThat(timers).hasSize(1);
        completeNext();

        verify(sender).send(fids, title, body);
        verify(sender).send(List.of(FID), title, body);
        verify(sender, never()).send(List.of(second), title, body);
        verify(sender, never()).send(List.of(third), title, body);
    }

    @Test
    @DisplayName("최초 호출 전체가 접수 불명확하면 원래 대상별로 한 번 더 보내고 반복 타임아웃은 종료한다")
    void retryInitialUncertainCallOncePerOriginalTarget() throws FirebaseMessagingException {
        now.set(Instant.now());
        String second = "d" + "a".repeat(21);
        List<String> fids = List.of(FID, second);
        String title = "새로운 댓글이 달렸어요!";
        String body = "댓글을 남겼던 문장의 이야기를 확인해 보세요.";
        var exception = mock(FirebaseMessagingException.class);
        given(exception.getErrorCode()).willReturn(ErrorCode.DEADLINE_EXCEEDED);
        var reader = mock(CommentNotificationRecipientReader.class);
        given(reader.findReadersInClub(5L, 13L, 7L)).willReturn(fids);
        given(registrations.snapshotForSend(fids)).willReturn(new NotificationRegistrations(List.of(
                registration(1L, FID), registration(2L, second))));
        given(sender.send(fids, title, body)).willThrow(exception);
        given(sender.send(List.of(FID), title, body)).willThrow(exception);
        var service = new CommentNotificationService(reader, sender, registrations, handler);
        var worker = new CommentNotificationWorker(service, handler, processor);

        worker.notifyClub(new ClubCommentCreated(11L, 5L, 13L, 7L, now.get()));
        assertThat(timers).hasSize(2);
        completeNext();
        completeNext();

        verify(sender).send(List.of(FID), title, body);
        verify(sender).send(List.of(second), title, body);
        assertThat(timers).isEmpty();
    }

    private NotificationRegistration registration(Long id, String fid) {
        return new NotificationRegistration(id, mock(Member.class), new FirebaseInstallationId(fid));
    }

    private void captureSchedules() {
        doAnswer(invocation -> {
            timers.addLast(invocation.getArgument(0));
            scheduledTimes.addLast(invocation.getArgument(1));
            return mock(ScheduledFuture.class);
        }).when(scheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    private void fireTimer() {
        Instant dueAt = scheduledTimes.removeFirst();
        if (now.get().isBefore(dueAt)) {
            now.set(dueAt);
        }
        timers.removeFirst().run();
    }

    private void completeNext() {
        fireTimer();
        workers.removeFirst().run();
    }

    private FcmRetryProcessor newProcessor(TaskExecutor executor, FcmFailureHandler failureHandler, int capacity) {
        return new FcmRetryProcessor(scheduler, executor, clock, sender, registrations, failureHandler, new Semaphore(capacity));
    }

    private FcmRetryTask task(FcmSendFailure failure, int retriesPerformed, boolean uncertainRetryUsed) {
        return new FcmRetryTask(11L, List.of(1L, 2L), TITLE, BODY, failure,
                CREATED_AT, now.get(), retriesPerformed, uncertainRetryUsed);
    }

    private FcmSendFailure internal(String retryAfter) {
        return new FcmSendFailure(FID, "INTERNAL", "INTERNAL", 500, retryAfter);
    }

    private FcmSendFailure uncertain() {
        return new FcmSendFailure(FID, "UNKNOWN", "DEADLINE_EXCEEDED", null, null);
    }

    private NotificationRegistrations snapshot(List<Long> ids, FirebaseInstallationId fid) {
        return new NotificationRegistrations(ids.stream()
                .map(id -> new NotificationRegistration(id, mock(Member.class), fid)).toList());
    }
}
