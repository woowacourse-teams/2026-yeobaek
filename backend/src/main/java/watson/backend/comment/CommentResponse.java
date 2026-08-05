package watson.backend.comment;

import java.time.LocalDateTime;

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
                comment.getCreatedAt(), comment.getUpdatedAt(), writerId.equals(requesterId));
    }
}
