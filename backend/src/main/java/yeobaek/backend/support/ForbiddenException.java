package yeobaek.backend.support;

import java.util.Map;
import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, String> logContext;

    public ForbiddenException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public ForbiddenException(ErrorCode code, String message, Map<String, String> logContext) {
        super(message);
        this.code = code;
        this.logContext = Map.copyOf(logContext);
    }
}
