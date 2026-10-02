package yeobaek.backend.club.api;

import yeobaek.backend.foundation.identity.MemberId;

@FunctionalInterface
public interface ClubMembershipApi {

    void erase(MemberId memberId);
}
