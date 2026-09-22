package yeobaek.backend.book.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ResolvedAuthors {

    private final List<Author> authors;

    public ResolvedAuthors(List<Author> authors) {
        this.authors = List.copyOf(authors);
    }

    public boolean containsUnsavedAuthor() {
        return authors.stream().anyMatch(author -> author.getId() == null);
    }

    public Set<Long> ids() {
        return authors.stream()
                .map(Author::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<Author> values() {
        return authors;
    }
}
