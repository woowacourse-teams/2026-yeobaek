package yeobaek.backend.application.club;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.space.api.club.ClubApi;
import yeobaek.backend.space.api.club.ClubMembershipApi;
import yeobaek.backend.collaboration.api.binding.ContentBindingNotFoundException;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.space.api.access.SpaceAccessApi;
import yeobaek.backend.application.space.query.SpaceQueryResultFactory;

@Service
@RequiredArgsConstructor
public class ClubMembershipWorkflow {

    private final MemberQuery memberQuery;
    private final SpaceAccessApi spaceAccessApi;
    private final SpaceContentBindingApi bindingApi;
    private final ContentApi contentApi;
    private final ClubMembershipApi memberships;
    private final ClubApi clubs;
    private final SpaceQueryResultFactory results;

    @Transactional
    public ClubCommandResult join(MemberId actorId, SpaceId spaceId) {
        memberQuery.getProfile(actorId);
        spaceAccessApi.getSpace(spaceId);
        var contents = bindingApi.findContents(spaceId);
        if (contents.isEmpty()) {
            throw new ContentBindingNotFoundException(
                    "가입할 모임에 연결된 컨텐츠가 없습니다: spaceId=" + spaceId.value(),
                    java.util.Map.of("spaceId", Long.toString(spaceId.value())));
        }
        contents.forEach(contentApi::requireAvailable);
        memberships.join(actorId, spaceId);
        var club = clubs.findBySpaceId(spaceId).orElseThrow();
        return new ClubCommandResult(club, results.content(contents.get(0)));
    }

    @Transactional
    public void leave(MemberId actorId, SpaceId spaceId) {
        memberQuery.getProfile(actorId);
        memberships.leave(actorId, spaceId);
    }
}
