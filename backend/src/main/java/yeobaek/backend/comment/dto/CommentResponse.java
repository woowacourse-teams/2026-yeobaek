package yeobaek.backend.comment.dto;

import java.time.LocalDateTime;
import yeobaek.backend.comment.domain.Comment;

public record CommentResponse(
        Long commentId,
        Long memberId,
        String nickname,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean mine
) {

    public static CommentResponse of(Comment comment, Long requesterId) {
        Long writerId = comment.getClubMember().getMember().getId();
        return new CommentResponse(comment.getId(), writerId,
                comment.getClubMember().getMember().getNickname(), comment.getContent(),
                comment.getCreatedAt(), comment.getUpdatedAt(), comment.isWrittenBy(requesterId));
    }
}
