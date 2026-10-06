package yeobaek.backend.content.api.body;

import java.util.Map;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.shared.identity.ContentId;

public final class ContentBodyUnsupportedException extends LogContextException {

    public ContentBodyUnsupportedException(ContentId contentId, ContentKind kind, String message, Throwable cause) {
        super(ErrorCode.INVALID_REQUEST, message,
                Map.of("contentId", Long.toString(contentId.value()), "kind", kind.value()), cause);
    }
}
