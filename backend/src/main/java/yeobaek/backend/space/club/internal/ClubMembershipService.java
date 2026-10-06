package yeobaek.backend.space.club.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceNotFoundException;
import yeobaek.backend.space.api.club.ClubMembershipApi;
import yeobaek.backend.space.api.club.ClubMembershipNotFoundException;
import yeobaek.backend.space.club.domain.ClubMember;
import yeobaek.backend.space.club.repository.ClubMemberCount;
import yeobaek.backend.space.club.repository.ClubMemberRepository;
import yeobaek.backend.space.club.repository.ClubRepository;
import yeobaek.backend.space.club.repository.MemberClubCount;
import yeobaek.backend.shared.identity.MemberId;

@Service
@RequiredArgsConstructor
public class ClubMembershipService implements ClubMembershipApi {

    private final ClubMemberRepository memberships;
    private final ClubRepository clubs;

    @Override
    @Transactional
    public void join(MemberId actorId, SpaceId spaceId) {
        var club = findClub(spaceId);
        memberships.findByMemberIdAndClubId(actorId.value(), club.getId())
                .ifPresentOrElse(ClubMember::rejoin,
                        () -> memberships.save(new ClubMember(actorId, club)));
    }

    @Override
    @Transactional
    public void leave(MemberId actorId, SpaceId spaceId) {
        var club = findClub(spaceId);
        memberships.findByMemberIdAndClubId(actorId.value(), club.getId())
                .orElseThrow(() -> new ClubMembershipNotFoundException(actorId, spaceId,
                        "탈퇴할 모임 가입 이력이 없습니다: actorId=" + actorId.value()
                                + ", spaceId=" + spaceId.value())).leave();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasMembershipHistory(MemberId memberId, Long clubId) {
        return memberships.findByMemberIdAndClubId(memberId.value(), clubId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipResponse> findJoinedByMember(MemberId memberId) {
        return memberships.findAllJoinedWithClubByMemberId(memberId.value()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipResponse> findJoinedByClub(Long clubId) {
        return memberships.findAllJoinedByClubId(clubId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countJoinedMembers(List<Long> clubIds) {
        return memberships.countJoinedMembersByClubIds(clubIds).stream()
                .collect(Collectors.toUnmodifiableMap(ClubMemberCount::getClubId, ClubMemberCount::getMemberCount));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> countJoinedClubs(List<MemberId> memberIds) {
        return memberships.countJoinedClubsByMemberIds(memberIds.stream().map(MemberId::value).toList()).stream()
                .collect(Collectors.toUnmodifiableMap(MemberClubCount::getMemberId, MemberClubCount::getClubCount));
    }

    @Override
    public void erase(MemberId memberId) {
        memberships.deleteAllByMemberId(memberId.value());
    }

    private yeobaek.backend.space.club.persistence.Club findClub(SpaceId spaceId) {
        return clubs.findBySpaceRootId(spaceId.value())
                .orElseThrow(() -> new SpaceNotFoundException(spaceId,
                        "참여할 모임 공간이 존재하지 않습니다: spaceId=" + spaceId.value()));
    }

    private MembershipResponse toResponse(ClubMember membership) {
        var club = membership.getClub();
        var response = new ClubSnapshot(new SpaceId(club.getSpaceId()), club.getId(), club.getName(),
                club.getJoinCode());
        return new MembershipResponse(new MemberId(membership.getMemberId()), response);
    }
}
