package yeobaek.backend.comment.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public static List<CommentedSentence> summarize(
            List<? extends CommentData> comments,
            Set<Long> viewedCommentIds
    ) {
        Map<Long, List<CommentData>> commentsBySentence = new LinkedHashMap<>();
        comments.forEach(comment -> commentsBySentence
                .computeIfAbsent(comment.sentenceId(), ignored -> new ArrayList<>())
                .add(comment));
        return commentsBySentence.values().stream()
                .map(sentenceComments -> from(sentenceComments, viewedCommentIds))
                .toList();
    }

    private static CommentedSentence from(List<CommentData> comments, Set<Long> viewedCommentIds) {
        CommentData representative = comments.getFirst();
        long unreadCommentCount = comments.stream()
                .filter(comment -> !viewedCommentIds.contains(comment.commentId()))
                .count();
        LocalDateTime latestCommentCreatedAt = comments.stream()
                .map(CommentData::createdAt)
                .max(LocalDateTime::compareTo)
                .orElseThrow();
        return new CommentedSentence(
                representative.sentenceId(),
                representative.sentenceContent(),
                representative.passageId(),
                new ContentSequence(representative.passageSequence()),
                new ContentSequence(representative.sentenceSequence()),
                comments.size(),
                unreadCommentCount,
                latestCommentCreatedAt);
    }

    public interface CommentData {

        Long commentId();

        LocalDateTime createdAt();

        Long sentenceId();

        String sentenceContent();

        Long passageId();

        int passageSequence();

        int sentenceSequence();
    }
}
