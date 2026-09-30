package yeobaek.backend.support;

import java.util.Map;
import lombok.Getter;

@Getter
public class InvalidRequestException extends IllegalArgumentException {

    private final Map<String, String> logContext;

    public InvalidRequestException(String message, Map<String, String> logContext) {
        super(message);
        this.logContext = Map.copyOf(logContext);
    }

    public InvalidRequestException(String message, Throwable cause, Map<String, String> logContext) {
        super(message, cause);
        this.logContext = Map.copyOf(logContext);
    }
}
