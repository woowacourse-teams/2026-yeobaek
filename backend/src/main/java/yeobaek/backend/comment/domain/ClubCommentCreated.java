package yeobaek.backend.comment.domain;

import java.time.Instant;

public record ClubCommentCreated(
        Long commentId, Long clubId, Long sentenceId, Long writerId, Instant commentCreatedAt
) {
}
