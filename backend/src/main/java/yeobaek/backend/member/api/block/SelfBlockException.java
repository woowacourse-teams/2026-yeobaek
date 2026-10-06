package yeobaek.backend.member.api.block;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.member.api.MemberException;

public final class SelfBlockException extends MemberException {

    public SelfBlockException(MemberId memberId, String message) {
        super(memberId, ErrorCode.CANNOT_BLOCK_SELF, message,
                Map.of("memberId", Long.toString(memberId.value()), "reason", "SELF_BLOCK"));
    }
}
