package yeobaek.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import com.google.firebase.ErrorCode;
import com.google.firebase.IncomingHttpResponse;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import yeobaek.backend.notification.domain.FcmSendFailure;
import yeobaek.backend.support.LogCapture;

class FcmPushSenderTest {

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FcmMessageFactory messageFactory = spy(new FcmMessageFactory());
    private final FcmPushSender sender = new FcmPushSender(messaging, messageFactory);

    @Test
    @DisplayName("모든 대상이 접수되면 INFO로 건수를 기록하고 빈 실패 목록을 반환한다")
    void logSuccessfulBatch() throws FirebaseMessagingException {
        List<String> fids = fids(2);
        BatchResponse response = response(success(), success());
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        try (var logs = new LogCapture(FcmPushSender.class.getName())) {
            var result = sender.send(fids, "댓글 알림", "비공개 댓글 본문");
            assertThat(result.failures()).isEmpty();

            var event = logs.event("notification.fcm.send", "success");
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(List.of(logs.field(event, "successCount"), logs.field(event, "failureCount")))
                    .containsExactly(2, 0);
        }
    }

    @Test
    @DisplayName("부분 실패는 WARN으로 성공·실패 수를 기록하고 실패 코드들을 반환한다")
    void logPartialFailure() throws FirebaseMessagingException {
        List<String> fids = fids(4);
        BatchResponse response = response(success(), failure(MessagingErrorCode.UNREGISTERED),
                failure(MessagingErrorCode.UNREGISTERED), failure(MessagingErrorCode.UNAVAILABLE));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        try (var logs = new LogCapture(FcmPushSender.class.getName())) {
            var result = sender.send(fids, "댓글 알림", "비공개 댓글 본문");

            var event = logs.event("notification.fcm.send", "failure");
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(List.of(logs.field(event, "successCount"), logs.field(event, "failureCount")))
                    .containsExactly(1, 3);
            assertThat(result.failures()).extracting(FcmSendFailure::errorCode)
                    .containsExactly("UNREGISTERED", "UNREGISTERED", "UNAVAILABLE");
        }
    }

    @Test
    @DisplayName("메시지에 넣은 순서대로 응답을 연결해 성공 대상은 빼고 실패한 FID만 반환한다")
    void mapFailuresToRequestOrder() throws FirebaseMessagingException {
        List<String> fids = List.of(fid(2), fid(0), fid(3), fid(1));
        BatchResponse response = response(success(), failure(MessagingErrorCode.UNREGISTERED),
                success(), failure(MessagingErrorCode.UNAVAILABLE));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        var result = sender.send(fids, "댓글 알림", "비공개 댓글 본문");

        ArgumentCaptor<List<String>> orderedFids = ArgumentCaptor.captor();
        verify(messageFactory).createMulticastMessage(
                orderedFids.capture(), eq("댓글 알림"), eq("비공개 댓글 본문"));
        assertThat(orderedFids.getValue()).containsExactly(fid(2), fid(0), fid(3), fid(1));
        assertThat(result.failures()).containsExactly(
                new FcmSendFailure(fid(0), "UNREGISTERED", "UNKNOWN", null, null),
                new FcmSendFailure(fid(1), "UNAVAILABLE", "UNKNOWN", null, null));
        assertThat(List.of(result.successCount(), result.failureCount())).containsExactly(2, 2);
    }

    @Test
    @DisplayName("모든 대상이 실패해도 결과를 반환하고 로그에 FID·본문·예외를 출력하지 않는다")
    void logFailedBatchWithoutSensitiveValues() throws FirebaseMessagingException {
        List<String> fids = fids(2);
        BatchResponse response = response(failure(MessagingErrorCode.INTERNAL), failure(MessagingErrorCode.INTERNAL));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        try (var logs = new LogCapture(FcmPushSender.class.getName())) {
            var result = sender.send(fids, "댓글 알림", "비공개 댓글 본문");

            assertThat(result.failures()).containsExactly(
                    new FcmSendFailure(fid(0), "INTERNAL", "UNKNOWN", null, null),
                    new FcmSendFailure(fid(1), "INTERNAL", "UNKNOWN", null, null));
            var event = logs.event("notification.fcm.send", "failure");
            assertThat(List.of(logs.field(event, "successCount"), logs.field(event, "failureCount")))
                    .containsExactly(0, 2);
            assertThat(event.getThrowableProxy()).isNull();
            assertThat(logs.structuredText()).doesNotContain(fid(0), "비공개 댓글 본문");
        }
    }

    @Test
    @DisplayName("HTTP 응답 없는 통신 오류는 FCM 코드가 UNKNOWN이어도 SDK 일반 코드를 보존한다")
    void preserveTransportErrorWithoutHttpResponse() throws FirebaseMessagingException {
        List<String> fids = fids(1);
        BatchResponse response = response(failure(null, ErrorCode.DEADLINE_EXCEEDED, null));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        try (var logs = new LogCapture(FcmPushSender.class.getName())) {
            var result = sender.send(fids, "댓글 알림", "비공개 댓글 본문");

            assertThat(result.failures()).containsExactly(
                    new FcmSendFailure(fid(0), "UNKNOWN", "DEADLINE_EXCEEDED", null, null));
            var event = logs.event("notification.fcm.send", "failure");
            assertThat(logs.field(event, "failureCount")).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("실패별 HTTP 상태와 SDK 코드 및 Retry-After 원문을 보존하고 헤더 대소문자를 구분하지 않는다")
    void preserveHttpMetadataForEachFailure() throws FirebaseMessagingException {
        String retryDate = "Wed, 21 Oct 2015 07:28:00 GMT";
        IncomingHttpResponse quotaResponse = httpResponse(429, Map.of("Retry-After", List.of("120")));
        IncomingHttpResponse unavailableResponse = httpResponse(503, Map.of("retry-after", retryDate));
        BatchResponse response = response(
                failure(MessagingErrorCode.QUOTA_EXCEEDED, ErrorCode.RESOURCE_EXHAUSTED, quotaResponse),
                failure(MessagingErrorCode.UNAVAILABLE, ErrorCode.UNAVAILABLE, unavailableResponse));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        var result = sender.send(fids(2), "댓글 알림", "비공개 댓글 본문");

        assertThat(result.failures()).containsExactly(
                new FcmSendFailure(fid(0), "QUOTA_EXCEEDED", "RESOURCE_EXHAUSTED", 429, "120"),
                new FcmSendFailure(fid(1), "UNAVAILABLE", "UNAVAILABLE", 503, retryDate));
    }

    @Test
    @DisplayName("FCM 상세 코드와 Retry-After가 없어도 SDK 인증 오류와 HTTP 상태를 보존한다")
    void preserveHttpErrorWithoutMessagingCodeOrRetryAfter() throws FirebaseMessagingException {
        IncomingHttpResponse httpResponse = httpResponse(401, Map.of());
        BatchResponse response = response(failure(null, ErrorCode.UNAUTHENTICATED, httpResponse));
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        var result = sender.send(fids(1), "댓글 알림", "비공개 댓글 본문");

        assertThat(result.failures()).containsExactly(
                new FcmSendFailure(fid(0), "UNKNOWN", "UNAUTHENTICATED", 401, null));
    }

    @Test
    @DisplayName("응답 없이 예외가 발생하면 기존 예외를 전파하고 접수 결과를 기록하지 않는다")
    void propagateExceptionWithoutInventingBatchResult() throws FirebaseMessagingException {
        List<String> fids = fids(1);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willThrow(exception);

        try (var logs = new LogCapture(FcmPushSender.class.getName())) {
            assertThatThrownBy(() -> sender.send(fids, "댓글 알림", "비공개 댓글 본문")).isSameAs(exception);
            assertThat(logs.events("notification.fcm.send")).isNotEmpty()
                    .allSatisfy(event -> assertThat(logs.hasField(event, "result")).isFalse());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    @DisplayName("요청 대상 수와 응답 수가 다르면 실패 FID를 임의로 연결하지 않는다")
    void rejectMismatchedResponseCount(int responseCount) throws FirebaseMessagingException {
        List<String> fids = fids(2);
        SendResponse[] outcomes = IntStream.range(0, responseCount)
                .mapToObj(index -> failure(MessagingErrorCode.INTERNAL)).toArray(SendResponse[]::new);
        BatchResponse response = response(outcomes);
        given(messaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);

        try (var logs = new LogCapture(FcmPushSender.class.getName())) {
            assertThatThrownBy(() -> sender.send(fids, "댓글 알림", "비공개 댓글 본문"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("FCM 요청 대상 수와 응답 수가 일치하지 않습니다.");
            assertThat(logs.events("notification.fcm.send")).isNotEmpty()
                    .allSatisfy(event -> assertThat(logs.hasField(event, "result")).isFalse());
        }
    }

    private List<String> fids(int targetCount) {
        return IntStream.range(0, targetCount).mapToObj(this::fid).toList();
    }

    private String fid(int index) {
        return "c" + "a".repeat(20) + index;
    }

    private BatchResponse response(SendResponse... outcomes) {
        List<SendResponse> results = List.of(outcomes);
        int successes = (int) results.stream().filter(SendResponse::isSuccessful).count();
        BatchResponse response = mock(BatchResponse.class);
        given(response.getResponses()).willReturn(results);
        given(response.getSuccessCount()).willReturn(successes);
        return response;
    }

    private IncomingHttpResponse httpResponse(int status, Map<String, Object> headers) {
        IncomingHttpResponse response = mock(IncomingHttpResponse.class);
        given(response.getStatusCode()).willReturn(status);
        given(response.getHeaders()).willReturn(headers);
        return response;
    }

    private SendResponse success() {
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(true);
        return response;
    }

    private SendResponse failure(MessagingErrorCode errorCode) {
        return failure(errorCode, ErrorCode.UNKNOWN, null);
    }

    private SendResponse failure(MessagingErrorCode errorCode, ErrorCode sdkErrorCode,
                                 IncomingHttpResponse httpResponse) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(errorCode);
        given(exception.getErrorCode()).willReturn(sdkErrorCode);
        given(exception.getHttpResponse()).willReturn(httpResponse);
        SendResponse response = mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(false);
        given(response.getException()).willReturn(exception);
        return response;
    }
}
