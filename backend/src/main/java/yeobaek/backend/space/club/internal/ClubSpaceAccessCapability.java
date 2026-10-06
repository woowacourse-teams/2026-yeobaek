package yeobaek.backend.space.club.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.space.club.repository.ClubMemberRepository;
import yeobaek.backend.space.club.repository.ClubRepository;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.internal.access.SpaceAccessCapability;
import yeobaek.backend.space.api.SpaceKind;

@Component
@RequiredArgsConstructor
public class ClubSpaceAccessCapability implements SpaceAccessCapability {

    private final ClubMemberRepository membershipRepository;
    private final ClubRepository clubRepository;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.CLUB;
    }

    @Override
    public yeobaek.backend.space.api.Space getSpace(SpaceId spaceId) {
        var club = clubRepository.findBySpaceRootId(spaceId.value()).orElseThrow();
        return new ClubSnapshot(spaceId, club.getId(), club.getName(), club.getJoinCode());
    }

    @Override
    public boolean canAccess(MemberId memberId, SpaceId spaceId) {
        return membershipRepository.existsJoinedByMemberIdAndSpaceId(memberId.value(), spaceId.value());
    }
}
