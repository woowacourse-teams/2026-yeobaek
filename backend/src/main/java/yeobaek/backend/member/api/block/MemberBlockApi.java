package yeobaek.backend.member.api.block;

import java.util.Map;
import java.util.Set;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.domain.MemberProfile;

public interface MemberBlockApi {

    Map<MemberId, MemberProfile> findBlockedProfiles(MemberId blockerId);

    Set<MemberId> findBlockedMemberIds(MemberId blockerId, Set<MemberId> memberIds);

    void block(MemberId blockerId, MemberId blockedId);

    void unblock(MemberId blockerId, MemberId blockedId);

    void eraseAllInvolving(MemberId memberId);
}
