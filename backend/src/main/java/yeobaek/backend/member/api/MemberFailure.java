package yeobaek.backend.member.api;

import yeobaek.backend.foundation.identity.MemberId;

public abstract class MemberFailure extends RuntimeException {

    private final MemberFailureReason failureReason;
    private final long affectedMemberId;

    protected MemberFailure(MemberFailureReason reason, MemberId memberId, String message) {
        super(message);
        this.failureReason = reason;
        this.affectedMemberId = memberId.value();
    }

    public MemberFailureReason reason() {
        return failureReason;
    }

    public MemberId memberId() {
        return new MemberId(affectedMemberId);
    }
}
