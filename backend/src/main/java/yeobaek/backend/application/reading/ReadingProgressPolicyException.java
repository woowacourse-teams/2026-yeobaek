package yeobaek.backend.application.reading;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class ReadingProgressPolicyException extends LogContextException {

    public ReadingProgressPolicyException(ErrorCode code, String message, Map<String, String> logContext) {
        super(code, message, logContext);
    }
}
