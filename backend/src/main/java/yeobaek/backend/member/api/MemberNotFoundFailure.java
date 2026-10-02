package yeobaek.backend.member.api;

import yeobaek.backend.foundation.identity.MemberId;

public final class MemberNotFoundFailure extends MemberFailure {

    public MemberNotFoundFailure(MemberId memberId) {
        super(MemberFailureReason.MEMBER_NOT_FOUND, memberId,
                "회원이 존재하지 않습니다: memberId=" + memberId.value());
    }
}
