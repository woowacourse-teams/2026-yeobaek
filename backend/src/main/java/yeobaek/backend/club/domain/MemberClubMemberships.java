package yeobaek.backend.club.domain;

import java.util.List;

public final class MemberClubMemberships {

    private final List<ClubMember> memberships;

    public MemberClubMemberships(List<ClubMember> memberships) {
        this.memberships = List.copyOf(memberships);
    }

    public List<Long> clubIds() {
        return memberships.stream()
                .map(clubMember -> clubMember.getClub().getId())
                .toList();
    }

    public List<Long> bookIds() {
        return memberships.stream()
                .map(clubMember -> clubMember.getClub().getBook().getId())
                .distinct()
                .toList();
    }

    public List<ClubMember> values() {
        return memberships;
    }
}
