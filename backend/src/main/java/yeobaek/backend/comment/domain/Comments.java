package yeobaek.backend.comment.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class Comments {

    private final List<Comment> values;

    public Comments(List<Comment> values) {
        this.values = List.copyOf(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }

    public Comment representativeComment() {
        if (isEmpty()) {
            throw new IllegalArgumentException("댓글이 없어 대표 댓글을 선택할 수 없습니다.");
        }
        return values.getFirst();
    }

    public long countUnread(Set<Long> viewedCommentIds) {
        return values.stream()
                .filter(comment -> !comment.isViewed(viewedCommentIds))
                .count();
    }

    public LocalDateTime latestCreatedAt() {
        return values.stream()
                .map(Comment::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    public List<Long> ids() {
        return values.stream()
                .map(Comment::getId)
                .toList();
    }

    public List<Comment> excludingIds(Set<Long> excludedIds) {
        return values.stream()
                .filter(comment -> !excludedIds.contains(comment.getId()))
                .toList();
    }

    public Comments excludingWrittenBy(Set<Long> memberIds) {
        return new Comments(values.stream()
                .filter(comment -> !comment.isWrittenByAny(memberIds))
                .toList());
    }

    public List<CommentedSentence> summarizeBySentence(Set<Long> viewedCommentIds) {
        Map<Long, List<Comment>> commentsBySentence = values.stream()
                .collect(Collectors.groupingBy(
                        Comment::getSentenceId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        return commentsBySentence.values().stream()
                .map(Comments::new)
                .map(comments -> CommentedSentence.from(comments, viewedCommentIds))
                .toList();
    }

    public List<Comment> asList() {
        return values;
    }
}
