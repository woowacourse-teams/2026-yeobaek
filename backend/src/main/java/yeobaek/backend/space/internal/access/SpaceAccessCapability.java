package yeobaek.backend.space.internal.access;

import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.Space;
import yeobaek.backend.space.api.SpaceKind;

public interface SpaceAccessCapability {

    SpaceKind supportedKind();

    Space getSpace(SpaceId spaceId);

    boolean canAccess(MemberId memberId, SpaceId spaceId);
}
