package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BookTest {

    @Test
    @DisplayName("같은 id를 가진 도서는 동일한 도서로 판단한다")
    void isSameWhenIdMatches() {
        Book book = new Book("제목", null, null, 1);
        Book other = new Book("다른 제목", null, null, 2);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(other, "id", 1L);

        assertThat(book.isSame(other)).isTrue();
    }

    @Test
    @DisplayName("id가 다른 도서는 동일한 도서가 아니라고 판단한다")
    void isNotSameWhenIdDiffers() {
        Book book = new Book("제목", null, null, 1);
        Book other = new Book("제목", null, null, 1);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(other, "id", 2L);

        assertThat(book.isSame(other)).isFalse();
    }

    @Test
    @DisplayName("제목·출판사·출판연도가 모두 같으면 동일 서지로 판단한다")
    void hasSameBibliography() {
        Book book = new Book("운수 좋은 날", "자체 제작", 1924, 1);

        assertThat(book.hasSameBibliography(new Book("운수 좋은 날", "자체 제작", 1924, 9))).isTrue();
        assertThat(book.hasSameBibliography(new Book("운수 좋은 날", null, 1924, 1))).isFalse();
        assertThat(book.hasSameBibliography(new Book("운수 좋은 날", "자체 제작", 1936, 1))).isFalse();
    }

    @Test
    @DisplayName("제목이 공백이거나 100자를 넘으면 거부한다")
    void rejectInvalidTitle() {
        assertThatThrownBy(() -> new Book(" ", null, null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Book("가".repeat(101), null, null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("출판사가 공백이거나 100자를 넘으면 거부한다")
    void rejectInvalidPublisher() {
        assertThatThrownBy(() -> new Book("제목", " ", null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Book("제목", "가".repeat(101), null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
