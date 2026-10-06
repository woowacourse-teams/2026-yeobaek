package yeobaek.backend.application.space.query;

import java.util.List;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;

public interface SpaceQueryProvider {

    SpaceKind supportedKind();

    List<SpaceQueryResult> findMine(MemberId actorId);

    SpaceQueryResult findDetail(MemberId actorId, SpaceId spaceId);
}
