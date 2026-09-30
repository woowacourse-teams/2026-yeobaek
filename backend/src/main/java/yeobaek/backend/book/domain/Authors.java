package yeobaek.backend.book.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Authors {

    private final List<Author> values;

    public Authors(List<Author> values) {
        this.values = List.copyOf(values);
    }

    public boolean containsUnsavedAuthor() {
        return values.stream().anyMatch(author -> author.getId() == null);
    }

    public Set<Long> ids() {
        return values.stream()
                .map(Author::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<Author> asList() {
        return values;
    }
}
