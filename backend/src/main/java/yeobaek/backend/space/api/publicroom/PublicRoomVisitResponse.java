package yeobaek.backend.space.api.publicroom;

import java.time.LocalDateTime;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

public record PublicRoomVisitResponse(MemberId actorId, SpaceId spaceId, LocalDateTime lastVisitedAt) {
}
