package yeobaek.backend.application.club;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.club.api.ClubCommandApi;
import yeobaek.backend.club.domain.vo.ClubName;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.space.domain.Club;

@Service
@RequiredArgsConstructor
public class ClubCreationWorkflow {

    private final MemberQuery memberQuery;
    private final ContentApi contentApi;
    private final ClubCommandApi clubCommandApi;
    private final SpaceContentBindingApi bindingApi;

    @Transactional
    public Club create(MemberId actorId, ClubName name, ContentId contentId) {
        memberQuery.getProfile(actorId);
        contentApi.requireAvailable(contentId);
        Club club = clubCommandApi.create(name);
        bindingApi.bind(club.id(), contentId);
        clubCommandApi.join(actorId, club.id());
        return club;
    }
}
