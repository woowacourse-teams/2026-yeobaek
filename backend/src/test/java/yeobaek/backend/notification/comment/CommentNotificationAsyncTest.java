package yeobaek.backend.notification.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import yeobaek.backend.comment.domain.ClubCommentCreated;
import yeobaek.backend.comment.domain.PublicRoomCommentCreated;
import yeobaek.backend.notification.FcmFailureHandler;
import yeobaek.backend.notification.FcmRetryProcessor;
import yeobaek.backend.notification.domain.FcmRetryTask;
import yeobaek.backend.support.LogCapture;
import yeobaek.backend.support.notification.NotificationExecutorConfiguration;

class CommentNotificationAsyncTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-19T00:00:00Z");

    private final CommentNotificationService service = mock(CommentNotificationService.class);
    private final FcmFailureHandler failureHandler = mock(FcmFailureHandler.class);
    private ThreadPoolTaskExecutor executor;
    private CommentNotificationListener listener;

    @BeforeEach
    void setUp() {
        executor = new NotificationExecutorConfiguration().notificationTaskExecutor(1, 1);
        executor.initialize();
        listener = new CommentNotificationListener(new CommentNotificationWorker(service, failureHandler, mock(FcmRetryProcessor.class)), executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("발송이 막혀 있어도 요청의 제출 호출은 돌아오고 별도 워커에서 실행한다")
    void returnWithoutWaitingForSend(boolean club) throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var finished = new CountDownLatch(1);
        var workerThread = new AtomicReference<String>();
        org.mockito.stubbing.Answer<List<FcmRetryTask>> blockedSend = invocation -> {
            workerThread.set(Thread.currentThread().getName());
            started.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("test release timed out");
                }
                return List.of();
            } finally {
                finished.countDown();
            }
        };
        if (club) {
            doAnswer(blockedSend).when(service).notifyReadersInClub(11L, 5L, 13L, 7L, CREATED_AT);
        } else {
            doAnswer(blockedSend).when(service).notifyPreviousCommentersInPublicRoom(11L, 8L, 13L, 7L, CREATED_AT);
        }

        try (var requests = Executors.newSingleThreadExecutor()) {
            var submission = requests.submit(() -> notify(club));
            try {
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                submission.get(2, TimeUnit.SECONDS);
                assertThat(finished.getCount()).isEqualTo(1);
                assertThat(workerThread.get()).startsWith("notification-");
            } finally {
                release.countDown();
            }
            assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("실제 풀이 포화되면 호출자 실행 없이 WARN을 남기고 제출을 거절한다")
    void rejectSaturatedQueueWithoutCallerRuns() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondFinished = new CountDownLatch(1);
        var firstCall = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (firstCall.getAndSet(false)) {
                started.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("test release timed out");
                }
            } else {
                secondFinished.countDown();
            }
            return List.of();
        }).when(service).notifyReadersInClub(anyLong(), eq(5L), eq(13L), eq(7L), eq(CREATED_AT));

        try (var logs = new LogCapture(CommentNotificationListener.class.getName())) {
            listener.onClubCommentCreated(new ClubCommentCreated(11L, 5L, 13L, 7L, CREATED_AT));
            try {
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                listener.onClubCommentCreated(new ClubCommentCreated(12L, 5L, 13L, 7L, CREATED_AT));
                listener.onClubCommentCreated(new ClubCommentCreated(13L, 5L, 13L, 7L, CREATED_AT));

                var event = logs.event("notification.comment.submit", "rejected");
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(logs.field(event, "commentId")).isEqualTo(13L);
            } finally {
                release.countDown();
            }
            assertThat(secondFinished.await(5, TimeUnit.SECONDS)).isTrue();
        }
        verify(service, times(2)).notifyReadersInClub(anyLong(), eq(5L), eq(13L), eq(7L), eq(CREATED_AT));
    }

    @Test
    @DisplayName("워커의 예기치 않은 예외는 댓글 ID·요청 MDC와 함께 기록하고 다음 작업을 처리한다")
    void recordUnexpectedWorkerFailureAndContinue() throws Exception {
        doThrow(new IllegalStateException("unexpected failure"))
                .when(service).notifyReadersInClub(11L, 5L, 13L, 7L, CREATED_AT);
        var finished = new CountDownLatch(1);
        var previousMdc = MDC.getCopyOfContextMap();

        try (var logs = new LogCapture(CommentNotificationListener.class.getName())) {
            MDC.put("traceId", "request-trace");
            try {
                notify(true);
            } finally {
                if (previousMdc == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previousMdc);
                }
            }
            executor.execute(finished::countDown);
            assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();

            var event = logs.event("notification.comment.logUnexpectedFailure", "failure");
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.field(event, "commentId")).isEqualTo(11L);
            assertThat(event.getMDCPropertyMap()).containsEntry("traceId", "request-trace");
        }
    }

    private void notify(boolean club) {
        if (club) {
            listener.onClubCommentCreated(new ClubCommentCreated(11L, 5L, 13L, 7L, CREATED_AT));
        } else {
            listener.onPublicRoomCommentCreated(new PublicRoomCommentCreated(11L, 8L, 13L, 7L, CREATED_AT));
        }
    }
}
