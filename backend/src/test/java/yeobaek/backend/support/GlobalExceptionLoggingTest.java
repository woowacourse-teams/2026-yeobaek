package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GlobalExceptionLoggingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private LogCapture attachedAppender;

    @AfterEach
    void detachAppender() {
        if (attachedAppender != null) {
            attachedAppender.close();
        }
    }

    @Test
    @DisplayName("의도한 4xx 거절은 INFO와 안전한 예외 컨텍스트로 기록한다")
    void logExpectedRejection() {
        ListAppender<ILoggingEvent> appender = attachAppender();

        handler.handleNotFound(new NotFoundException(
                ErrorCode.CLUB_NOT_FOUND, "sensitive-message", Map.of("clubId", "7")));

        ILoggingEvent event = appender.list.getLast();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(keyValue(event, "phase")).isEqualTo("rejected");
        assertThat(keyValue(event, "clubId")).isEqualTo("7");
        assertThat(event.getFormattedMessage()).doesNotContain("sensitive-message");
    }

    @Test
    @DisplayName("의도하지 않은 5xx는 ERROR와 스택 트레이스로 기록한다")
    void logUnexpectedFailure() {
        ListAppender<ILoggingEvent> appender = attachAppender();
        IllegalStateException exception = new IllegalStateException("server-secret");

        handler.handleIllegalState(exception);

        ILoggingEvent event = appender.list.getLast();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(keyValue(event, "phase")).isEqualTo("failure");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getFormattedMessage()).doesNotContain("server-secret");
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        attachedAppender = new LogCapture(GlobalExceptionHandler.class.getName());
        return attachedAppender;
    }

    private Object keyValue(ILoggingEvent event, String key) {
        return event.getKeyValuePairs().stream()
                .filter(pair -> pair.key.equals(key))
                .map(pair -> pair.value)
                .findFirst()
                .orElse(null);
    }
}
