package yeobaek.backend.application.appreciation;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class CommentPolicyException extends LogContextException {

    public CommentPolicyException(ErrorCode code, String message, Map<String, String> logContext) {
        super(code, message, logContext);
    }
}
