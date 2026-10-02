package yeobaek.backend.member.api;

import yeobaek.backend.foundation.identity.MemberId;

@FunctionalInterface
public interface MemberDataEraser {

    void erase(MemberId memberId);
}
