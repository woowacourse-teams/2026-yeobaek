package yeobaek.backend.comment.domain;

import java.util.List;
import java.util.Set;

public final class Comments {

    private final List<Comment> entries;

    public Comments(List<Comment> comments) {
        this.entries = List.copyOf(comments);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Long> ids() {
        return entries.stream()
                .map(Comment::getId)
                .toList();
    }

    public List<Comment> excludingIds(Set<Long> excludedIds) {
        return entries.stream()
                .filter(comment -> !excludedIds.contains(comment.getId()))
                .toList();
    }

    public List<Comment> values() {
        return entries;
    }
}
