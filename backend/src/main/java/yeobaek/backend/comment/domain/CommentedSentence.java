package yeobaek.backend.comment.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
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

    public static CommentedSentence from(List<Comment> comments, Set<Long> viewedCommentIds) {
        if (comments.isEmpty()) {
            throw new IllegalArgumentException("집계할 댓글이 필요합니다.");
        }
        Comment first = comments.getFirst();
        long unreadCommentCount = comments.stream()
                .filter(comment -> !comment.isViewed(viewedCommentIds))
                .count();
        LocalDateTime latestCommentCreatedAt = comments.stream()
                .map(Comment::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElseThrow();
        Sentence sentence = first.getSentence();
        Passage passage = sentence.getPassage();
        return new CommentedSentence(
                sentence.getId(),
                sentence.getContent(),
                passage.getId(),
                passage.getSequence(),
                sentence.getSequence(),
                comments.size(),
                unreadCommentCount,
                latestCommentCreatedAt);
    }
}
