package yeobaek.backend.collaboration.api.binding;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public class ContentBindingNotFoundException extends LogContextException {

    public ContentBindingNotFoundException(String message, Map<String, String> logContext) {
        super(ErrorCode.CONTENT_NOT_BOUND, message, logContext);
    }
}
