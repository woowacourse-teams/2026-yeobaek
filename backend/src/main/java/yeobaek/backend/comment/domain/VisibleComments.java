package yeobaek.backend.comment.domain;

import java.util.List;
import java.util.Set;

public final class VisibleComments {

    private final List<Comment> comments;

    public VisibleComments(List<Comment> comments) {
        this.comments = List.copyOf(comments);
    }

    public boolean isEmpty() {
        return comments.isEmpty();
    }

    public List<Long> ids() {
        return comments.stream()
                .map(Comment::getId)
                .toList();
    }

    public List<Comment> excludingIds(Set<Long> excludedIds) {
        return comments.stream()
                .filter(comment -> !excludedIds.contains(comment.getId()))
                .toList();
    }

    public List<Comment> values() {
        return comments;
    }
}
