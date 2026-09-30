package yeobaek.backend.club.domain;

import java.util.List;
import java.util.Optional;

public final class ClubMembers {

    private final List<ClubMember> values;

    public ClubMembers(List<ClubMember> values) {
        this.values = List.copyOf(values);
    }

    public Optional<ClubMember> findByMemberId(Long memberId) {
        return values.stream()
                .filter(clubMember -> clubMember.isOwnedBy(memberId))
                .findFirst();
    }

    public List<Long> memberIds() {
        return values.stream()
                .map(clubMember -> clubMember.getMember().getId())
                .toList();
    }

    public List<Long> clubIds() {
        return values.stream()
                .map(clubMember -> clubMember.getClub().getId())
                .toList();
    }

    public List<Long> bookIds() {
        return values.stream()
                .map(clubMember -> clubMember.getClub().getBook().getId())
                .distinct()
                .toList();
    }

    public List<ClubMember> asList() {
        return values;
    }
}
