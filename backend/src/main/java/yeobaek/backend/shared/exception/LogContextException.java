package yeobaek.backend.shared.exception;

import java.util.Map;
import java.util.Objects;
import lombok.Getter;

@Getter
public abstract class LogContextException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, String> logContext;

    protected LogContextException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    protected LogContextException(ErrorCode code, String message, Map<String, String> logContext) {
        this(code, message, logContext, null);
    }

    protected LogContextException(ErrorCode code, String message, Map<String, String> logContext, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.logContext = Map.copyOf(Objects.requireNonNull(logContext, "logContext"));
    }
}
