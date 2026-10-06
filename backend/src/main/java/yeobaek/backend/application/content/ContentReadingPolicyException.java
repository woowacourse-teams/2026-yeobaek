package yeobaek.backend.application.content;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class ContentReadingPolicyException extends LogContextException {

    public ContentReadingPolicyException(ErrorCode code, String message, Map<String, String> logContext) {
        super(code, message, logContext);
    }
}
