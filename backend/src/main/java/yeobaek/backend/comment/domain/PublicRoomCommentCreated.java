package yeobaek.backend.comment.domain;

import java.time.Instant;

public record PublicRoomCommentCreated(
        Long commentId, Long publicRoomId, Long sentenceId, Long writerId, Instant commentCreatedAt
) {
}
