package yeobaek.backend.application.club;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.club.ClubMembershipApi;
import yeobaek.backend.space.api.club.ClubName;
import yeobaek.backend.space.api.club.ClubResponse;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.application.space.query.SpaceQueryResultFactory;

@Service
@RequiredArgsConstructor
public class ClubCreationWorkflow {

    private final MemberQuery memberQuery;
    private final ContentApi contentApi;
    private final ClubApi clubApi;
    private final ClubMembershipApi membershipApi;
    private final SpaceContentBindingApi bindingApi;
    private final SpaceQueryResultFactory results;

    @Transactional
    public ClubCommandResult create(MemberId actorId, ClubName name, ContentId contentId) {
        memberQuery.getProfile(actorId);
        contentApi.requireAvailable(contentId);
        ClubResponse club = clubApi.create(name);
        bindingApi.bind(club.id(), contentId);
        membershipApi.join(actorId, club.id());
        return new ClubCommandResult(club, results.content(contentId));
    }
}
