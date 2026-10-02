package yeobaek.backend.member.api;

import yeobaek.backend.foundation.identity.MemberId;

public final class SelfBlockFailure extends MemberFailure {

    public SelfBlockFailure(MemberId memberId) {
        super(MemberFailureReason.SELF_BLOCK, memberId,
                "자기 자신은 차단할 수 없습니다: memberId=" + memberId.value());
    }
}
