package yeobaek.backend.book.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookDuplicateCriteriaTest {

    @Test
    @DisplayName("제목·출판사·출판연도·작가 집합이 모두 같으면 중복으로 판단한다")
    void duplicateWhenAllCriteriaMatch() {
        BookDuplicateCriteria criteria = criteria("출판사", 1924, Set.of(1L, 2L));
        BookDuplicateCriteria reorderedAuthors = criteria("출판사", 1924, Set.of(2L, 1L));

        assertThat(criteria.isDuplicateOf(reorderedAuthors)).isTrue();
        assertThat(criteria.isDuplicateOf(criteria("다른 출판사", 1924, Set.of(1L, 2L)))).isFalse();
        assertThat(criteria.isDuplicateOf(criteria("출판사", 1936, Set.of(1L, 2L)))).isFalse();
        assertThat(criteria.isDuplicateOf(criteria("출판사", 1924, Set.of(1L)))).isFalse();
        assertThat(criteria.isDuplicateOf(criteria("출판사", 1924, Set.of(1L, 3L)))).isFalse();
    }

    @Test
    @DisplayName("출판사와 출판연도가 없는 도서도 같은 기준으로 중복을 판단한다")
    void duplicateWithNullableValues() {
        BookDuplicateCriteria criteria = criteria(null, null, Set.of(1L));

        assertThat(criteria.isDuplicateOf(criteria(null, null, Set.of(1L)))).isTrue();
        assertThat(criteria.isDuplicateOf(criteria("출판사", null, Set.of(1L)))).isFalse();
    }

    @Test
    @DisplayName("생성 이후 외부의 작가 집합 변경은 중복 판단 기준에 영향을 주지 않는다")
    void defensiveCopyAuthorIds() {
        Set<Long> authorIds = new HashSet<>(Set.of(1L));
        BookDuplicateCriteria criteria = criteria("출판사", 1924, authorIds);

        authorIds.add(2L);

        assertThat(criteria.authorIds()).containsExactly(1L);
    }

    @Test
    @DisplayName("출판 정보와 작가가 같아도 제목이 다르면 중복이 아니다")
    void differentTitleIsNotDuplicate() {
        BookDuplicateCriteria criteria = criteria("출판사", 1924, Set.of(1L));
        BookDuplicateCriteria other = new BookDuplicateCriteria(
                new BookTitle("다른 작품"), new Publisher("출판사"), 1924, Set.of(1L));

        assertThat(criteria.isDuplicateOf(other)).isFalse();
    }

    private BookDuplicateCriteria criteria(String publisher, Integer publishedYear, Set<Long> authorIds) {
        return new BookDuplicateCriteria(
                new BookTitle("운수 좋은 날"),
                publisher == null ? null : new Publisher(publisher),
                publishedYear,
                authorIds);
    }
}
