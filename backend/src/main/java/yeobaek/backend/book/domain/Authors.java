package yeobaek.backend.book.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Authors {

    private final List<Author> entries;

    public Authors(List<Author> authors) {
        this.entries = List.copyOf(authors);
    }

    public boolean containsUnsavedAuthor() {
        return entries.stream().anyMatch(author -> author.getId() == null);
    }

    public Set<Long> ids() {
        return entries.stream()
                .map(Author::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<Author> values() {
        return entries;
    }
}
