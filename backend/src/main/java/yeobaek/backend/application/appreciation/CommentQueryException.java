package yeobaek.backend.application.appreciation;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class CommentQueryException extends LogContextException {

    public CommentQueryException(ErrorCode code, String message, Map<String, String> context) {
        super(code, message, context);
    }
}
