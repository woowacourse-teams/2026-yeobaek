package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogContextExceptionTest {

    @Test
    @DisplayName("공통 로그 맥락 예외는 코드, 메시지와 로그 맥락을 제공한다")
    void provideExceptionDetails() {
        var exception = new TestLogContextException(
                ErrorCode.INVALID_REQUEST, "잘못된 요청", Map.of("memberId", "1"));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(exception.getMessage()).isEqualTo("잘못된 요청");
        assertThat(exception.getLogContext()).containsExactly(Map.entry("memberId", "1"));
    }

    @Test
    @DisplayName("공통 로그 맥락 예외의 기본 생성자는 빈 로그 맥락을 제공한다")
    void provideEmptyContextByDefault() {
        var exception = new TestLogContextException(ErrorCode.INVALID_REQUEST, "잘못된 요청");

        assertThat(exception.getLogContext()).isEmpty();
    }

    @Test
    @DisplayName("개별 코드의 잘못된 요청 예외도 원인 예외와 로그 맥락을 보존한다")
    void preserveBadRequestCauseAndContext() {
        var cause = new IllegalStateException("DB 제약 위반");
        var exception = new BadRequestException(
                ErrorCode.DUPLICATE_NICKNAME, "이미 사용 중인 닉네임입니다.",
                cause, Map.of("reason", "duplicate_nickname"));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
        assertThat(exception.getMessage()).isEqualTo("이미 사용 중인 닉네임입니다.");
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getLogContext()).containsExactly(Map.entry("reason", "duplicate_nickname"));
    }

    private static final class TestLogContextException extends LogContextException {

        private TestLogContextException(ErrorCode code, String message) {
            super(code, message);
        }

        private TestLogContextException(ErrorCode code, String message, Map<String, String> logContext) {
            super(code, message, logContext);
        }
    }
}
