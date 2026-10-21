package yeobaek.backend.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import yeobaek.backend.comment.domain.Comment;

public record CommentResponse(
        @Schema(description = "댓글 ID") Long commentId,
        @Schema(description = "작성자 회원 ID") Long memberId,
        @Schema(description = "작성자 닉네임") String nickname,
        @Schema(description = "내용") String content,
        @Schema(description = "작성일") LocalDateTime createdAt,
        @Schema(description = "수정일 (수정된 적 없으면 null)", nullable = true) LocalDateTime updatedAt,
        @Schema(description = "요청자 본인 작성 여부") boolean mine,
        @Schema(description = "댓글 좋아요 총개수") long likeCount,
        @Schema(description = "요청 회원의 좋아요 여부") boolean likedByMe
) {

    public static CommentResponse withoutLike(Comment comment, Long requesterId) {
        return of(comment, requesterId, 0, false);
    }

    public static CommentResponse of(Comment comment, Long requesterId, long likeCount, boolean likedByMe) {
        Long writerId = comment.getWriter().getId();
        return new CommentResponse(comment.getId(), writerId,
                comment.getWriter().getNickname(), comment.getContent(),
                comment.getCreatedAt(), comment.getUpdatedAt(), comment.isWrittenBy(requesterId), likeCount, likedByMe);
    }
}
