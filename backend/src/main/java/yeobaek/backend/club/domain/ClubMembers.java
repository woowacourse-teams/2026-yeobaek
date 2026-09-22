package yeobaek.backend.club.domain;

import java.util.List;
import java.util.Optional;

public final class ClubMembers {

    private final List<ClubMember> members;

    public ClubMembers(List<ClubMember> members) {
        this.members = List.copyOf(members);
    }

    public Optional<ClubMember> findByMemberId(Long memberId) {
        return members.stream()
                .filter(clubMember -> clubMember.isOwnedBy(memberId))
                .findFirst();
    }

    public List<Long> memberIds() {
        return members.stream()
                .map(clubMember -> clubMember.getMember().getId())
                .toList();
    }

    public List<Long> clubIds() {
        return members.stream()
                .map(clubMember -> clubMember.getClub().getId())
                .toList();
    }

    public List<Long> bookIds() {
        return members.stream()
                .map(clubMember -> clubMember.getClub().getBook().getId())
                .distinct()
                .toList();
    }

    public List<ClubMember> values() {
        return members;
    }
}
