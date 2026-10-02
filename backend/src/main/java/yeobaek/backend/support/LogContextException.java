package yeobaek.backend.support;

import java.util.Map;
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
        this.code = code;
        this.logContext = Map.copyOf(logContext);
    }
}
