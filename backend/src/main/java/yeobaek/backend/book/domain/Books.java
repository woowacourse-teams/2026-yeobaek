package yeobaek.backend.book.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import yeobaek.backend.book.domain.vo.BookDeduplicationKey;

public final class Books {

    private final List<Book> values;

    public Books(List<Book> values) {
        this.values = List.copyOf(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<Long> ids() {
        return values.stream()
                .map(Book::getId)
                .toList();
    }

    public boolean containsDuplicateOf(BookDeduplicationKey key, Map<Long, Set<Long>> authorIdsByBookId) {
        return values.stream()
                .map(book -> book.deduplicationKey(authorIdsByBookId.getOrDefault(book.getId(), Set.of())))
                .anyMatch(key::isDuplicateOf);
    }
}
