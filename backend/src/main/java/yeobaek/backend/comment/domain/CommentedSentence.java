package yeobaek.backend.comment.domain;

import java.time.LocalDateTime;
import java.util.Set;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.Sentence;
import yeobaek.backend.book.domain.vo.ContentSequence;

public record CommentedSentence(
        Long sentenceId,
        String content,
        Long passageId,
        ContentSequence passageSequence,
        ContentSequence sentenceSequence,
        long commentCount,
        long unreadCommentCount,
        LocalDateTime latestCommentCreatedAt
) {

    public boolean isFutureOf(Passage currentPassage) {
        return passageSequence.isAfter(currentPassage.getSequence());
    }

    public static CommentedSentence from(Comments comments, Set<Long> viewedCommentIds) {
        Comment representativeComment = comments.representativeComment();
        Sentence sentence = representativeComment.getSentence();
        Passage passage = sentence.getPassage();
        return new CommentedSentence(
                sentence.getId(),
                sentence.getContent(),
                passage.getId(),
                passage.getSequence(),
                sentence.getSequence(),
                comments.size(),
                comments.countUnread(viewedCommentIds),
                comments.latestCreatedAt());
    }
}
