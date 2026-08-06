package watson.backend.book;

import static org.assertj.core.api.Assertions.assertThat;

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
}
