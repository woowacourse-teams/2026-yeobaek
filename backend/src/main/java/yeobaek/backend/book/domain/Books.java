package yeobaek.backend.book.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import yeobaek.backend.book.domain.vo.BookDuplicateCriteria;

public final class Books {

    private final List<Book> entries;

    public Books(List<Book> books) {
        this.entries = List.copyOf(books);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Long> ids() {
        return entries.stream()
                .map(Book::getId)
                .toList();
    }

    public boolean containsDuplicateOf(BookDuplicateCriteria criteria, Map<Long, Set<Long>> authorIdsByBookId) {
        return entries.stream()
                .map(book -> book.duplicateCriteria(authorIdsByBookId.getOrDefault(book.getId(), Set.of())))
                .anyMatch(criteria::isDuplicateOf);
    }
}
