package yeobaek.backend.space.api;

import yeobaek.backend.foundation.identity.SpaceId;

public final class SpaceNotFoundFailure extends RuntimeException {

    private final long failedSpaceIdValue;

    public SpaceNotFoundFailure(SpaceId spaceId) {
        super("공간이 존재하지 않습니다: spaceId=" + spaceId.value());
        this.failedSpaceIdValue = spaceId.value();
    }

    public long failedSpaceId() {
        return failedSpaceIdValue;
    }
}
