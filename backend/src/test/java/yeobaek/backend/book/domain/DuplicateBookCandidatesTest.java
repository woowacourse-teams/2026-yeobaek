package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.vo.BookDuplicateCriteria;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.Publisher;

class DuplicateBookCandidatesTest {

    @Test
    @DisplayName("중복 검사 후보 도서 id를 반환한다")
    void ids() {
        Book first = bookWithId(1L, "운수 좋은 날");
        Book second = bookWithId(2L, "날개");
        DuplicateBookCandidates candidates = new DuplicateBookCandidates(List.of(first, second));

        assertThat(candidates.ids()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("서지 정보와 작가 구성이 같은 도서가 있는지 확인한다")
    void containsDuplicateOf() {
        Book candidate = bookWithId(1L, "운수 좋은 날");
        DuplicateBookCandidates candidates = new DuplicateBookCandidates(List.of(candidate));
        BookDuplicateCriteria criteria = candidate.duplicateCriteria(Set.of(10L));

        boolean duplicate = candidates.containsDuplicateOf(criteria, Map.of(1L, Set.of(10L)));

        assertThat(duplicate).isTrue();
    }

    @Test
    @DisplayName("작가 구성이 다르면 중복 도서가 아니다")
    void doesNotContainDifferentAuthors() {
        Book candidate = bookWithId(1L, "운수 좋은 날");
        DuplicateBookCandidates candidates = new DuplicateBookCandidates(List.of(candidate));
        BookDuplicateCriteria criteria = candidate.duplicateCriteria(Set.of(10L));

        boolean duplicate = candidates.containsDuplicateOf(criteria, Map.of(1L, Set.of(20L)));

        assertThat(duplicate).isFalse();
    }

    private Book bookWithId(Long id, String title) {
        Book book = new Book(new BookTitle(title), new Publisher("출판사"), 2026, 1, null);
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
