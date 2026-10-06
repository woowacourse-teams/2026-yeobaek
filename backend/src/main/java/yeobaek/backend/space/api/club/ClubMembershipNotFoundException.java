package yeobaek.backend.space.api.club;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

public class ClubMembershipNotFoundException extends LogContextException {

    public ClubMembershipNotFoundException(MemberId actorId, SpaceId spaceId, String message) {
        super(ErrorCode.NOT_CLUB_MEMBER, message,
                Map.of("actorId", Long.toString(actorId.value()), "spaceId", Long.toString(spaceId.value())));
    }
}
