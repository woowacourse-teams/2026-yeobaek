package yeobaek.backend.space.api;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.shared.identity.SpaceId;

public final class SpaceNotFoundException extends LogContextException {

    private final long failedSpaceIdValue;

    public SpaceNotFoundException(SpaceId spaceId, String message) {
        super(ErrorCode.SPACE_NOT_FOUND, message, Map.of("spaceId", Long.toString(spaceId.value())));
        this.failedSpaceIdValue = spaceId.value();
    }

    public long failedSpaceId() {
        return failedSpaceIdValue;
    }
}
