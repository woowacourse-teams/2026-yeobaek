package yeobaek.backend.content.api.legacyreference;

import java.util.Map;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class ContentLegacyReferenceNotFoundException extends LogContextException {

    private final transient ContentKind missingContentKind;
    private final long missingLegacyId;

    public ContentLegacyReferenceNotFoundException(ContentKind kind, long legacyId, String message) {
        super(ErrorCode.CONTENT_NOT_FOUND, message,
                Map.of("kind", kind.value(), "legacyId", Long.toString(legacyId)));
        this.missingContentKind = kind;
        this.missingLegacyId = legacyId;
    }

    public ContentKind contentKind() {
        return missingContentKind;
    }

    public long failedLegacyId() {
        return missingLegacyId;
    }
}
