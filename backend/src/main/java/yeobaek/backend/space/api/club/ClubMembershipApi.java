package yeobaek.backend.space.api.club;

import java.util.List;
import java.util.Map;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

public interface ClubMembershipApi {

    void join(MemberId actorId, SpaceId spaceId);

    void leave(MemberId actorId, SpaceId spaceId);

    boolean hasMembershipHistory(MemberId memberId, Long clubId);

    List<MembershipResponse> findJoinedByMember(MemberId memberId);

    List<MembershipResponse> findJoinedByClub(Long clubId);

    Map<Long, Long> countJoinedMembers(List<Long> clubIds);

    Map<Long, Long> countJoinedClubs(List<MemberId> memberIds);

    void erase(MemberId memberId);

    record MembershipResponse(MemberId memberId, ClubResponse club) {
    }
}
