package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GlobalExceptionLoggingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("의도한 4xx 거절은 INFO와 안전한 예외 컨텍스트로 기록한다")
    void logExpectedRejection() {
        try (var logs = new LogCapture(GlobalExceptionHandler.class.getName())) {
            handler.handleNotFound(new NotFoundException(
                    ErrorCode.CLUB_NOT_FOUND, "sensitive-message", Map.of("clubId", "7")));

            var event = logs.event("exception.handleNotFound", "rejected");
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(logs.field(event, "errorCode")).isEqualTo("CLUB_NOT_FOUND");
            assertThat(logs.field(event, "clubId")).isEqualTo("7");
            assertThat(event.getFormattedMessage()).doesNotContain("sensitive-message");
        }
    }

    @Test
    @DisplayName("모임 도서에 속하지 않는 문장은 전용 오류 코드로 기록한다")
    void distinguishSentenceOutsideClubBook() {
        try (var logs = new LogCapture(GlobalExceptionHandler.class.getName())) {
            handler.handleNotFound(new NotFoundException(
                    ErrorCode.SENTENCE_NOT_IN_CLUB_BOOK, "sensitive-message",
                    Map.of("clubId", "5", "sentenceId", "13")));

            var event = logs.event("exception.handleNotFound", "rejected");
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(logs.field(event, "errorCode")).isEqualTo("SENTENCE_NOT_IN_CLUB_BOOK");
            assertThat(logs.field(event, "clubId")).isEqualTo("5");
            assertThat(logs.field(event, "sentenceId")).isEqualTo("13");
        }
    }

    @Test
    @DisplayName("인증 거절 로그는 안전한 사유를 포함하고 원문 토큰을 제외한다")
    void logSafeAuthenticationRejection() {
        try (var logs = new LogCapture(GlobalExceptionHandler.class.getName())) {
            handler.handleUnauthorized(new UnauthorizedException(
                    ErrorCode.UNAUTHORIZED, "X-Admin-Token=raw-admin-token",
                    Map.of("reason", "invalid_admin_token")));

            var event = logs.event("exception.handleUnauthorized", "rejected");
            assertThat(logs.field(event, "errorCode")).isEqualTo("UNAUTHORIZED");
            assertThat(logs.field(event, "reason")).isEqualTo("invalid_admin_token");
            assertThat(event.getThrowableProxy()).isNull();
            assertThat(logs.structuredText()).doesNotContain("X-Admin-Token", "raw-admin-token");
        }
    }

    @Test
    @DisplayName("IllegalStateException은 공통 5xx 처리로 ERROR와 원래 예외를 기록한다")
    void logUnexpectedFailure() {
        IllegalStateException exception = new IllegalStateException("server-secret");

        try (var logs = new LogCapture(GlobalExceptionHandler.class.getName())) {
            handler.handleException(exception);

            var event = logs.event("exception.handleException", "failure");
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.field(event, "errorCode")).isEqualTo("INTERNAL_ERROR");
            assertThat(logs.field(event, "exceptionType")).isEqualTo("IllegalStateException");
            assertThat(((ThrowableProxy) event.getThrowableProxy()).getThrowable()).isSameAs(exception);
        }
    }

    @Test
    @DisplayName("처리되지 않은 예외는 공통 작업명과 원래 예외로 ERROR 기록한다")
    void logUnhandledException() {
        Exception exception = new Exception("original-cause");

        try (var logs = new LogCapture(GlobalExceptionHandler.class.getName())) {
            handler.handleException(exception);

            var event = logs.event("exception.handleException", "failure");
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.field(event, "errorCode")).isEqualTo("INTERNAL_ERROR");
            assertThat(logs.field(event, "exceptionType")).isEqualTo("Exception");
            assertThat(((ThrowableProxy) event.getThrowableProxy()).getThrowable()).isSameAs(exception);
        }
    }
}
