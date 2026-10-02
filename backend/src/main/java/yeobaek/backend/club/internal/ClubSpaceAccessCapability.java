package yeobaek.backend.club.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.club.repository.ClubMemberRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.api.SpaceAccessCapability;
import yeobaek.backend.space.domain.Club;

@Component
@RequiredArgsConstructor
public class ClubSpaceAccessCapability implements SpaceAccessCapability {

    private final ClubMemberRepository membershipRepository;
    private final ClubRepository clubRepository;

    @Override
    public String supportedKind() {
        return Club.KIND;
    }

    @Override
    public yeobaek.backend.space.domain.Space getSpace(SpaceId spaceId) {
        var club = clubRepository.findBySpaceRootId(spaceId.value()).orElseThrow();
        return new Club(spaceId, club.getId(), club.getName(), club.getJoinCode());
    }

    @Override
    public boolean canAccess(MemberId memberId, SpaceId spaceId) {
        return membershipRepository.existsJoinedByMemberIdAndSpaceId(memberId.value(), spaceId.value());
    }
}
