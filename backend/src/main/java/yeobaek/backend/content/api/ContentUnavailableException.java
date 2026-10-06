package yeobaek.backend.content.api;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.shared.identity.ContentId;

public final class ContentUnavailableException extends LogContextException {

    public ContentUnavailableException(ContentId contentId, String message) {
        super(ErrorCode.CONTENT_UNAVAILABLE, message, Map.of("contentId", Long.toString(contentId.value())));
    }
}
