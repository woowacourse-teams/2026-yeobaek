package yeobaek.backend.reading.api;

import java.time.LocalDateTime;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.reading.domain.PublicRoomVisit;

public interface PublicRoomVisitApi {

    PublicRoomVisit visit(MemberId actorId, SpaceId spaceId, LocalDateTime visitedAt);

    void erase(MemberId actorId);
}
