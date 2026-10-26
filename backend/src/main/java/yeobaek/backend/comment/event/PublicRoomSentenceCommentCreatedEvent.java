package yeobaek.backend.comment.event;

import java.time.Instant;

public record PublicRoomSentenceCommentCreatedEvent(
        Long commentId, Long publicRoomId, Long sentenceId, Long writerId, int passageSequence, Instant commentCreatedAt
) {
}
