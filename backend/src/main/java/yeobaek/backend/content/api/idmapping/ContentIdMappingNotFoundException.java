package yeobaek.backend.content.api.idmapping;

import java.util.Map;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class ContentIdMappingNotFoundException extends LogContextException {

    private final transient ContentKind missingContentKind;
    private final long missingImplementationId;

    public ContentIdMappingNotFoundException(ContentKind kind, long implementationId, String message) {
        super(ErrorCode.CONTENT_NOT_FOUND, message,
                Map.of("kind", kind.value(), "legacyId", Long.toString(implementationId)));
        this.missingContentKind = kind;
        this.missingImplementationId = implementationId;
    }

    public ContentKind contentKind() {
        return missingContentKind;
    }

    public long failedImplementationId() {
        return missingImplementationId;
    }
}
