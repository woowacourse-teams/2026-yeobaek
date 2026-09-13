package yeobaek.backend.comment.dto;

import java.time.LocalDateTime;
import yeobaek.backend.comment.domain.ContentVisibility;

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
