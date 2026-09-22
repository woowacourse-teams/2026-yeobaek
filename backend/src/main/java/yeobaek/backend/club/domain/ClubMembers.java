package yeobaek.backend.club.domain;

import java.util.List;
import java.util.Optional;

public final class ClubMembers {

    private final List<ClubMember> members;

    private ClubMembers(List<ClubMember> values) {
        this.members = List.copyOf(values);
    }

    public static ClubMembers from(List<ClubMember> values) {
        return new ClubMembers(values);
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

    public List<ClubMember> values() {
        return members;
    }
}
