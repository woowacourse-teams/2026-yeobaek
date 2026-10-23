package yeobaek.backend.comment.repository;

public record CommentLikeSummary(Long commentId, long likeCount, boolean likedByMe) {
}
