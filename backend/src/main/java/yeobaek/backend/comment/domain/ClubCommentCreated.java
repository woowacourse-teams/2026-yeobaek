package yeobaek.backend.comment.domain;

public record ClubCommentCreated(Long commentId, Long clubId, Long sentenceId, Long writerId) {
}
