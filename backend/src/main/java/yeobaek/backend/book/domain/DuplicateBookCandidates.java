package yeobaek.backend.book.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import yeobaek.backend.book.domain.vo.BookDuplicateCriteria;

public final class DuplicateBookCandidates {

    private final List<Book> books;

    public DuplicateBookCandidates(List<Book> books) {
        this.books = List.copyOf(books);
    }

    public boolean isEmpty() {
        return books.isEmpty();
    }

    public List<Long> ids() {
        return books.stream()
                .map(Book::getId)
                .toList();
    }

    public boolean containsDuplicateOf(BookDuplicateCriteria criteria, Map<Long, Set<Long>> authorIdsByBookId) {
        return books.stream()
                .map(book -> book.duplicateCriteria(authorIdsByBookId.getOrDefault(book.getId(), Set.of())))
                .anyMatch(criteria::isDuplicateOf);
    }
}
