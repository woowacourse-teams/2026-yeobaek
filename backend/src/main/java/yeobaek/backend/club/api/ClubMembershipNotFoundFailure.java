package yeobaek.backend.club.api;

import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;

public class ClubMembershipNotFoundFailure extends RuntimeException {

    public ClubMembershipNotFoundFailure(MemberId actorId, SpaceId spaceId) {
        super("가입 이력이 없습니다: actorId=" + actorId.value() + ", spaceId=" + spaceId.value());
    }
}
