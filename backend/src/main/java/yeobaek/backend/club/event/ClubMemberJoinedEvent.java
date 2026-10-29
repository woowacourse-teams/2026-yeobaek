package yeobaek.backend.club.event;

import java.time.Instant;

public record ClubMemberJoinedEvent(Long clubId, Long memberId, Instant joinedAt) {
}
