package yeobaek.backend.space.api.publicroom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

public interface PublicRoomVisitApi {

    PublicRoomVisitResponse visit(MemberId actorId, SpaceId spaceId, LocalDateTime visitedAt);

    List<PublicRoomVisitResponse> findVisits(MemberId actorId);

    Optional<PublicRoomVisitResponse> findLastVisit(MemberId actorId, SpaceId spaceId);

    Map<SpaceId, Long> countVisitors(List<SpaceId> spaceIds);

    void erase(MemberId actorId);
}
