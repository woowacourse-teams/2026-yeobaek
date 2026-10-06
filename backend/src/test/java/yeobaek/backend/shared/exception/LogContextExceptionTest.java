package yeobaek.backend.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
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
    @DisplayName("공통 예외는 로그 맥락을 복사해 불변으로 제공한다")
    void copyImmutableContext() {
        Map<String, String> source = new HashMap<>();
        source.put("memberId", "1");
        var exception = new TestLogContextException(ErrorCode.INVALID_REQUEST, "잘못된 요청", source);

        source.put("memberId", "2");

        assertThat(exception.getLogContext()).containsExactly(Map.entry("memberId", "1"));
        assertThatThrownBy(() -> exception.getLogContext().put("spaceId", "3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("공통 예외는 원인 예외를 보존한다")
    void preserveCause() {
        var cause = new IllegalStateException("저장 실패");
        var exception = new TestLogContextException(
                ErrorCode.INTERNAL_ERROR, "처리할 수 없습니다", Map.of("operation", "save"), cause);

        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("공통 예외는 클라이언트 분기 코드를 필수로 요구한다")
    void rejectNullCode() {
        assertThatThrownBy(() -> {
            throw new TestLogContextException(null, "잘못된 요청");
        }).isInstanceOf(NullPointerException.class)
                .hasMessage("code");
    }

    private static final class TestLogContextException extends LogContextException {

        private TestLogContextException(ErrorCode code, String message) {
            super(code, message);
        }

        private TestLogContextException(ErrorCode code, String message, Map<String, String> logContext) {
            super(code, message, logContext);
        }

        private TestLogContextException(ErrorCode code, String message, Map<String, String> logContext,
                                        Throwable cause) {
            super(code, message, logContext, cause);
        }
    }
}
