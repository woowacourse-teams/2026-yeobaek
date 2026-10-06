package yeobaek.backend.application.space.query;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class SpaceQueryPolicyException extends LogContextException {

    public SpaceQueryPolicyException(ErrorCode code, String message, Map<String, String> logContext) {
        super(code, message, logContext);
    }
}
