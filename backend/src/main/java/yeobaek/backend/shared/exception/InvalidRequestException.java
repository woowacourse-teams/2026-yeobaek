package yeobaek.backend.shared.exception;

import java.util.Map;

public class InvalidRequestException extends LogContextException {

    public InvalidRequestException(String message, Map<String, String> logContext) {
        super(ErrorCode.INVALID_REQUEST, message, logContext);
    }

    public InvalidRequestException(String message, Throwable cause, Map<String, String> logContext) {
        super(ErrorCode.INVALID_REQUEST, message, logContext, cause);
    }
}
