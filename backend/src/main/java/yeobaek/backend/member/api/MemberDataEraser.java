package yeobaek.backend.member.api;

import yeobaek.backend.shared.identity.MemberId;

public interface MemberDataEraser {

    void erase(MemberId memberId);
}
