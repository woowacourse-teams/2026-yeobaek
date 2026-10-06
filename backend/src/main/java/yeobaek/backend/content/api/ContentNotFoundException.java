package yeobaek.backend.content.api;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.shared.identity.ContentId;

public final class ContentNotFoundException extends LogContextException {

    private final long failedContentIdValue;

    public ContentNotFoundException(ContentId contentId, String message) {
        super(ErrorCode.CONTENT_NOT_FOUND, message, Map.of("contentId", Long.toString(contentId.value())));
        this.failedContentIdValue = contentId.value();
    }

    public long failedContentId() {
        return failedContentIdValue;
    }
}
