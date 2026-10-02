package yeobaek.backend.application.club;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.club.api.ClubCommandApi;
import yeobaek.backend.collaboration.api.ContentBindingNotFoundFailure;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.space.api.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class ClubMembershipWorkflow {

    private final MemberQuery memberQuery;
    private final SpaceAccessApi spaceAccessApi;
    private final SpaceContentBindingApi bindingApi;
    private final ContentApi contentApi;
    private final ClubCommandApi clubCommandApi;

    @Transactional
    public void join(MemberId actorId, SpaceId spaceId) {
        memberQuery.getProfile(actorId);
        spaceAccessApi.getSpace(spaceId);
        var contents = bindingApi.findContents(spaceId);
        if (contents.isEmpty()) {
            throw new ContentBindingNotFoundFailure(spaceId);
        }
        contents.forEach(contentApi::requireAvailable);
        clubCommandApi.join(actorId, spaceId);
    }

    @Transactional
    public void leave(MemberId actorId, SpaceId spaceId) {
        memberQuery.getProfile(actorId);
        clubCommandApi.leave(actorId, spaceId);
    }
}
