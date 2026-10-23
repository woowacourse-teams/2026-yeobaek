package yeobaek.backend.comment.event;

import java.time.Instant;

public record ClubSentenceCommentCreatedEvent(
        Long commentId, Long clubId, Long sentenceId, Long writerId, int passageSequence, Instant commentCreatedAt
) {
}
