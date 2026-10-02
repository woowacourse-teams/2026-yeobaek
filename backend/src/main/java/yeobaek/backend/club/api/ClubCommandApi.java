package yeobaek.backend.club.api;

import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.domain.Club;

public interface ClubCommandApi {

    Club create(ClubName name);

    void join(MemberId actorId, SpaceId spaceId);

    void leave(MemberId actorId, SpaceId spaceId);
}
