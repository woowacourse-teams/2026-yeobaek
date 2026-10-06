package yeobaek.backend.member.api;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.shared.identity.MemberId;

public abstract class MemberException extends LogContextException {

    private final long affectedMemberId;

    protected MemberException(MemberId memberId, ErrorCode code,
                              String message, Map<String, String> logContext) {
        super(code, message, logContext);
        this.affectedMemberId = memberId.value();
    }

    public MemberId memberId() {
        return new MemberId(affectedMemberId);
    }

}
