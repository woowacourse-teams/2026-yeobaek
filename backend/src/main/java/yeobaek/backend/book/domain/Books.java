package yeobaek.backend.book.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import yeobaek.backend.book.domain.vo.BookDuplicateCriteria;

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

    public boolean containsDuplicateOf(BookDuplicateCriteria criteria, Map<Long, Set<Long>> authorIdsByBookId) {
        return values.stream()
                .map(book -> book.duplicateCriteria(authorIdsByBookId.getOrDefault(book.getId(), Set.of())))
                .anyMatch(criteria::isDuplicateOf);
    }
}
