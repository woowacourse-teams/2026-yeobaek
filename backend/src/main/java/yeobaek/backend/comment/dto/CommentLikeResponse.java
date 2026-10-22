package yeobaek.backend.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommentLikeResponse(
        @Schema(description = "댓글 좋아요 총개수") long likeCount,
        @Schema(description = "요청 회원의 좋아요 여부") boolean likedByMe
) {
}
