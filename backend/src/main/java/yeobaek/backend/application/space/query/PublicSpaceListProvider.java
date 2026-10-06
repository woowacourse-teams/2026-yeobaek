package yeobaek.backend.application.space.query;

import java.util.List;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.space.api.SpaceKind;

public interface PublicSpaceListProvider {

    SpaceKind supportedKind();

    List<SpaceQueryResult> findPublic(MemberId actorId);
}
