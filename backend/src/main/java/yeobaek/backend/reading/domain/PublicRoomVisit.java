package yeobaek.backend.reading.domain;

import java.time.LocalDateTime;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;

public record PublicRoomVisit(MemberId actorId, SpaceId spaceId, LocalDateTime lastVisitedAt) {
}
