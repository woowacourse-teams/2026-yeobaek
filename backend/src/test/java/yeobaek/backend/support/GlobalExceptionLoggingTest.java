package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.appreciation.internal.erasure.UnsupportedAppreciationKindException;
import yeobaek.backend.application.appreciation.CommentPolicyException;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.NotFoundException;
import yeobaek.backend.shared.exception.UnauthorizedException;

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
    @DisplayName("커스텀 예외의 호출자 로그 맥락을 메시지 파싱 없이 기록한다")
    void logCallerContext() {
        var exception = new CommentPolicyException(ErrorCode.SPACE_ACCESS_DENIED,
                "클라이언트 디버깅 메시지: spaceId=8",
                Map.of("actorId", "7", "spaceId", "8", "contentId", "9", "locationId", "10",
                        "reason", "SPACE_ACCESS_DENIED"));

        try (var logs = new LogCapture(GlobalExceptionHandler.class.getName())) {
            handler.handleCommentPolicyException(exception);

            var event = logs.event("exception.handleCommentPolicyFailure", "rejected");
            verifyCommentContext(logs, event);
            assertThat(event.getFormattedMessage()).doesNotContain("클라이언트 디버깅 메시지");
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

    @Test
    @DisplayName("별도 핸들러가 없는 예외도 ERROR와 원본 스택 및 원인을 기록한다")
    void logUnhandledException() {
        var cause = new IllegalStateException("underlying failure");
        var exception = new RuntimeException("unhandled failure", cause);

        try (var logs = new LogCapture(GlobalExceptionHandler.class.getName())) {
            handler.handleException(exception);

            var event = logs.event("exception.handleException", "failure");
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.field(event, "errorCode")).isEqualTo("INTERNAL_ERROR");
            assertThat(event.getThrowableProxy().getClassName()).isEqualTo(RuntimeException.class.getName());
            assertThat(event.getThrowableProxy().getStackTraceElementProxyArray()).isNotEmpty();
            verifyLoggedCause(event);
        }
    }

    @Test
    @DisplayName("커스텀 서버 예외는 ERROR와 원인 예외 및 구조화된 맥락을 함께 기록한다")
    void logCustomServerFailureContextAndCause() {
        var cause = new IllegalStateException("registry lookup failed");
        var exception = new UnsupportedAppreciationKindException(
                new AppreciationKind("NOTE"), "저장된 감상 타입을 삭제할 수 없습니다.", cause);

        try (var logs = new LogCapture(GlobalExceptionHandler.class.getName())) {
            handler.handleLogContextException(exception);

            var event = logs.event("exception.handleLogContextException", "failure");
            verifyServerFailure(logs, event);
            assertThat(exception.getCause()).isSameAs(cause);
        }
    }

    private void verifyCommentContext(LogCapture logs, ch.qos.logback.classic.spi.ILoggingEvent event) {
        assertThat(logs.field(event, "errorCode")).isEqualTo("SPACE_ACCESS_DENIED");
        assertThat(logs.field(event, "actorId")).isEqualTo("7");
        assertThat(logs.field(event, "spaceId")).isEqualTo("8");
        assertThat(logs.field(event, "contentId")).isEqualTo("9");
        assertThat(logs.field(event, "locationId")).isEqualTo("10");
    }

    private void verifyServerFailure(LogCapture logs, ch.qos.logback.classic.spi.ILoggingEvent event) {
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(logs.field(event, "errorCode")).isEqualTo("INTERNAL_ERROR");
        assertThat(logs.field(event, "kind")).isEqualTo("NOTE");
        assertThat(event.getThrowableProxy()).isNotNull();
        verifyLoggedCause(event);
    }

    private void verifyLoggedCause(ch.qos.logback.classic.spi.ILoggingEvent event) {
        assertThat(event.getThrowableProxy().getCause()).isNotNull();
        assertThat(event.getThrowableProxy().getCause().getClassName())
                .isEqualTo(IllegalStateException.class.getName());
    }
}
