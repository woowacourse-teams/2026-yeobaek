package yeobaek.backend.comment.repository;

import java.time.LocalDateTime;
import yeobaek.backend.comment.domain.CommentedSentence;

public record CommentSentenceRow(
        Long commentId,
        LocalDateTime createdAt,
        Long sentenceId,
        String sentenceContent,
        Long passageId,
        int passageSequence,
        int sentenceSequence
) implements CommentedSentence.CommentData {
}
