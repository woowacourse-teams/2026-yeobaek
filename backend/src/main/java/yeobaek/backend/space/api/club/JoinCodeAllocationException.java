package yeobaek.backend.space.api.club;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public class JoinCodeAllocationException extends LogContextException {

    public JoinCodeAllocationException(String message, Map<String, String> logContext) {
        super(ErrorCode.INTERNAL_ERROR, message, logContext);
    }
}
