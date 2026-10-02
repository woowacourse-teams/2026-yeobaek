package yeobaek.backend.member.api;

import java.util.Map;
import java.util.Set;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.member.domain.MemberProfile;

public interface MemberQuery {

    MemberProfile getProfile(MemberId memberId);

    Map<MemberId, MemberProfile> findProfiles(Set<MemberId> memberIds);
}
