package yeobaek.backend.comment.domain;

import java.util.List;
import java.util.Set;

public final class Comments {

    private final List<Comment> values;

    public Comments(List<Comment> values) {
        this.values = List.copyOf(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
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

    public List<Comment> asList() {
        return values;
    }
}
