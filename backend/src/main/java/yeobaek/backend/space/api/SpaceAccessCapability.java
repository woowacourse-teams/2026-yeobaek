package yeobaek.backend.space.api;

import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.domain.Space;

public interface SpaceAccessCapability {

    String supportedKind();

    Space getSpace(SpaceId spaceId);

    boolean canAccess(MemberId memberId, SpaceId spaceId);
}
