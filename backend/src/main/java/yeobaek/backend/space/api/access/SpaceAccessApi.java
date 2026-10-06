package yeobaek.backend.space.api.access;

import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.Space;

public interface SpaceAccessApi {

    Space getSpace(SpaceId spaceId);

    boolean canAccess(MemberId memberId, SpaceId spaceId);
}
