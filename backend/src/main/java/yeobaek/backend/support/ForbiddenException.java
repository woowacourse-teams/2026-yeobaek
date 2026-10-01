package yeobaek.backend.support;

import java.util.Map;

public class ForbiddenException extends LogContextException {

    public ForbiddenException(ErrorCode code, String message) {
        super(code, message);
    }

    public ForbiddenException(ErrorCode code, String message, Map<String, String> logContext) {
        super(code, message, logContext);
    }
}
