package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
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
    @DisplayName("의도하지 않은 5xx는 ERROR와 스택 트레이스로 기록한다")
    void logUnexpectedFailure() {
        IllegalStateException exception = new IllegalStateException("server-secret");

        try (var logs = new LogCapture(GlobalExceptionHandler.class.getName())) {
            handler.handleIllegalState(exception);

            var event = logs.event("exception.handleIllegalState", "failure");
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.field(event, "errorCode")).isEqualTo("INTERNAL_ERROR");
            assertThat(event.getThrowableProxy()).isNotNull();
        }
    }
}
