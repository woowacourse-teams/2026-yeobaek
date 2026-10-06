package yeobaek.backend.appreciation.api;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class AppreciationNotFoundException extends LogContextException {

    public AppreciationNotFoundException(String message, Map<String, String> logContext) {
        super(ErrorCode.APPRECIATION_NOT_FOUND, message, logContext);
    }
}
