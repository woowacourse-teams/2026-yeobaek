package yeobaek.backend.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class FcmSendFailureTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-19T00:00:00Z");

    @ParameterizedTest
    @CsvSource({
            "UNAVAILABLE, true",
            "INTERNAL, true",
            "QUOTA_EXCEEDED, true",
            "UNREGISTERED, false",
            "INVALID_ARGUMENT, false",
            "SENDER_ID_MISMATCH, false",
            "THIRD_PARTY_AUTH_ERROR, false",
            "UNKNOWN, false",
            "NEW_ERROR_CODE, false"
    })
    @DisplayName("일시 장애로 정한 세 코드만 재시도 후보로 판단한다")
    void identifyRetryableFailure(String errorCode, boolean retryable) {
        var failure = new FcmSendFailure("c" + "a".repeat(21), errorCode, "UNKNOWN", null, null);

        assertThat(failure.isRetryable()).isEqualTo(retryable);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "UNKNOWN,UNKNOWN,500,INTERNAL,true,false,false",
            "UNKNOWN,UNKNOWN,503,UNAVAILABLE,true,false,false",
            "UNKNOWN,RESOURCE_EXHAUSTED,429,QUOTA_EXCEEDED,true,false,false",
            "UNKNOWN,UNKNOWN,400,INVALID_ARGUMENT,false,false,false",
            "UNKNOWN,UNKNOWN,401,UNAUTHENTICATED,false,false,false",
            "UNKNOWN,UNKNOWN,403,PERMISSION_DENIED,false,false,false",
            "UNKNOWN,UNKNOWN,404,NOT_FOUND,false,false,false",
            "UNKNOWN,UNKNOWN,502,UNKNOWN,false,false,false",
            "UNKNOWN,UNKNOWN,504,UNKNOWN,false,false,false",
            "UNKNOWN,DEADLINE_EXCEEDED,null,RECEIPT_UNCERTAIN,true,true,false",
            "UNKNOWN,UNAVAILABLE,null,RECEIPT_UNCERTAIN,true,true,false",
            "UNKNOWN,UNAUTHENTICATED,null,UNAUTHENTICATED,false,false,false",
            "UNKNOWN,PERMISSION_DENIED,null,PERMISSION_DENIED,false,false,false",
            "UNKNOWN,INVALID_ARGUMENT,null,INVALID_ARGUMENT,false,false,false",
            "UNKNOWN,NOT_FOUND,null,NOT_FOUND,false,false,false",
            "UNKNOWN,INTERNAL,null,UNKNOWN,false,false,false",
            "UNKNOWN,RESOURCE_EXHAUSTED,null,UNKNOWN,false,false,false",
            "UNKNOWN,CANCELLED,null,UNKNOWN,false,false,false",
            "UNKNOWN,UNKNOWN,null,UNKNOWN,false,false,false",
            "UNREGISTERED,NOT_FOUND,404,UNREGISTERED,false,false,true",
            "UNREGISTERED,UNKNOWN,null,UNREGISTERED,false,false,true",
            "INVALID_ARGUMENT,INVALID_ARGUMENT,400,INVALID_ARGUMENT,false,false,false",
            "SENDER_ID_MISMATCH,PERMISSION_DENIED,403,SENDER_ID_MISMATCH,false,false,false",
            "THIRD_PARTY_AUTH_ERROR,UNAUTHENTICATED,401,THIRD_PARTY_AUTH_ERROR,false,false,false",
            "UNAVAILABLE,UNAVAILABLE,503,UNAVAILABLE,true,false,false",
            "INTERNAL,INTERNAL,500,INTERNAL,true,false,false",
            "UNREGISTERED,UNAUTHENTICATED,401,UNKNOWN,false,false,false",
            "UNKNOWN,UNAUTHENTICATED,503,UNKNOWN,false,false,false",
            "UNAVAILABLE,PERMISSION_DENIED,null,UNKNOWN,false,false,false",
            "UNKNOWN,CANCELLED,503,UNKNOWN,false,false,false",
            "UNKNOWN,DEADLINE_EXCEEDED,500,UNKNOWN,false,false,false",
            "INTERNAL,DEADLINE_EXCEEDED,null,UNKNOWN,false,false,false",
            "NEW_ERROR_CODE,INTERNAL,500,UNKNOWN,false,false,false"
    }, nullValues = "null")
    @DisplayName("상세 코드와 SDK 코드 및 HTTP 상태를 구분하고 충돌은 자동 조치하지 않는다")
    void classifyAvailableEvidence(String detail, String sdk, Integer status, String handlingCode,
                                   boolean retryable, boolean uncertain, boolean unregistered) {
        var failure = new FcmSendFailure("fid", detail, sdk, status, null);

        assertThat(failure.handlingCode()).isEqualTo(handlingCode);
        assertThat(failure.isRetryable()).isEqualTo(retryable);
        assertThat(failure.isReceiptUncertain()).isEqualTo(uncertain);
        assertThat(failure.isUnregistered()).isEqualTo(unregistered);
    }

    @ParameterizedTest
    @CsvSource({"0,60", "1,120", "2,240", "3,300", "4,300"})
    @DisplayName("추가 재시도는 1·2·4·5·5분의 기본 대기를 사용한다")
    void calculateBackoff(int retriesPerformed, long seconds) {
        assertThat(internal(null).nextRetryAt(CREATED_AT, CREATED_AT, retriesPerformed, false, 0))
                .contains(CREATED_AT.plusSeconds(seconds));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null,60", "'',60", "broken,60", "-1,60", "0,60", "30,60", "120,120",
            "' 120 ',120", "'Mon, 19 Oct 2026 00:05:00 GMT',300",
            "'Mon, 19 Oct 2026 00:00:00 GMT',60", "'Sun, 18 Oct 2026 23:00:00 GMT',60"
    }, nullValues = "null")
    @DisplayName("Retry-After는 초·날짜 형식을 해석하고 기본 대기보다 짧게 보내지 않는다")
    void respectRetryAfter(String retryAfter, long seconds) {
        assertThat(internal(retryAfter).nextRetryAt(CREATED_AT, CREATED_AT, 0, false, 0))
                .contains(CREATED_AT.plusSeconds(seconds));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null,false", "'',false", "120,false", "-1,true", "broken,true",
            "'Mon, 19 Oct 2026 00:05:00 GMT',false"
    }, nullValues = "null")
    @DisplayName("해석할 수 없는 Retry-After는 로그로 구분할 수 있다")
    void identifyInvalidRetryAfter(String retryAfter, boolean invalid) {
        assertThat(internal(retryAfter).hasInvalidRetryAfter()).isEqualTo(invalid);
    }

    @Test
    @DisplayName("무작위 지연은 서버가 요구한 대기 이후에 더한다")
    void addJitterAfterServerMinimum() {
        assertThat(internal("120").nextRetryAt(CREATED_AT, CREATED_AT, 0, false, 20))
                .contains(CREATED_AT.plusSeconds(132));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 6})
    @DisplayName("추가 5회를 이미 수행했다면 다음 재시도를 허용하지 않는다")
    void stopAtAttemptLimit(int retriesPerformed) {
        assertThat(internal(null).nextRetryAt(CREATED_AT, CREATED_AT, retriesPerformed, false, 0)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {1740, 1800, 1801})
    @DisplayName("현재나 다음 시도 시각이 댓글 생성 후 30분 이상이면 종료한다")
    void stopAtExpiry(long observedAfterSeconds) {
        assertThat(internal(null).nextRetryAt(
                CREATED_AT, CREATED_AT.plusSeconds(observedAfterSeconds), 0, false, 0)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1800", "3600", "9223372036854775807", "999999999999999999999999999999"})
    @DisplayName("유효기간을 넘거나 표현할 수 없는 긴 Retry-After를 짧게 줄여 재시도하지 않는다")
    void stopWhenServerDelayExceedsExpiry(String retryAfter) {
        assertThat(internal(retryAfter).nextRetryAt(CREATED_AT, CREATED_AT, 0, false, 20)).isEmpty();
    }

    @Test
    @DisplayName("지연을 더해 정확히 30분이 되는 경우에도 예약하지 않는다")
    void includeJitterInExpiryDecision() {
        assertThat(internal(null).nextRetryAt(
                CREATED_AT, CREATED_AT.plusSeconds(1728), 0, false, 20)).isEmpty();
    }

    @Test
    @DisplayName("접수 불명확에 따른 추가 시도는 전체 흐름에서 한 번만 허용한다")
    void retryUncertainReceiptOnlyOnce() {
        var failure = new FcmSendFailure("fid", "UNKNOWN", "DEADLINE_EXCEEDED", null, null);

        assertThat(failure.nextRetryAt(CREATED_AT, CREATED_AT, 0, false, 0))
                .contains(CREATED_AT.plusSeconds(60));
        assertThat(failure.nextRetryAt(CREATED_AT, CREATED_AT.plusSeconds(60), 1, true, 0)).isEmpty();
        assertThat(internal(null).nextRetryAt(CREATED_AT, CREATED_AT.plusSeconds(60), 1, true, 0))
                .contains(CREATED_AT.plusSeconds(180));
    }

    @Test
    @DisplayName("요청 오류에는 재시도 시각을 만들지 않는다")
    void doNotPlanPermanentFailure() {
        var failure = new FcmSendFailure("fid", "INVALID_ARGUMENT", "INVALID_ARGUMENT", 400, null);

        assertThat(failure.nextRetryAt(CREATED_AT, CREATED_AT, 0, false, 0)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "0,-1", "0,21"})
    @DisplayName("잘못된 횟수나 지연 비율을 거부한다")
    void rejectInvalidRetryHistory(int retriesPerformed, int jitterPercent) {
        var failure = internal(null);

        assertThatThrownBy(() -> failure.nextRetryAt(
                CREATED_AT, CREATED_AT, retriesPerformed, false, jitterPercent))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private FcmSendFailure internal(String retryAfter) {
        return new FcmSendFailure("fid", "INTERNAL", "INTERNAL", 500, retryAfter);
    }
}
