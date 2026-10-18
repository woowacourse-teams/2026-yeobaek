package yeobaek.backend.comment.domain;

public record PublicRoomCommentCreated(Long commentId, Long publicRoomId, Long sentenceId, Long writerId) {
}
