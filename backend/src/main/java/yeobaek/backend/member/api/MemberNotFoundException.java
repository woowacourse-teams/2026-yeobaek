package yeobaek.backend.member.api;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.identity.MemberId;

public final class MemberNotFoundException extends MemberException {

    public MemberNotFoundException(MemberId memberId, String message) {
        super(memberId, ErrorCode.MEMBER_NOT_FOUND, message,
                Map.of("memberId", Long.toString(memberId.value()), "reason", "MEMBER_NOT_FOUND"));
    }
}
