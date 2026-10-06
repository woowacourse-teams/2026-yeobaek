package yeobaek.backend.web.comment.dto;

import java.time.LocalDateTime;

public record CommentedSentenceResponse(
        Long sentenceId,
        String content,
        Long passageId,
        int passageSequence,
        int sentenceSequence,
        boolean future,
        long commentCount,
        long unreadCommentCount,
        ContentVisibility contentVisibility,
        LocalDateTime latestCommentCreatedAt
) {
}
