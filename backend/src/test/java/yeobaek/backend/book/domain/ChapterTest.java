package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.ChapterTitle;

class ChapterTest {

    @Test
    @DisplayName("장이 속한 도서와 같은 도서를 전달하면 참을 반환한다")
    void belongsToOwnBook() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);
        ReflectionTestUtils.setField(book, "id", 1L);
        Chapter chapter = new Chapter(book, new ChapterTitle("1장"), 1);

        assertThat(chapter.belongsTo(book)).isTrue();
    }

    @Test
    @DisplayName("장이 속하지 않은 도서를 전달하면 거짓을 반환한다")
    void doesNotBelongToOtherBook() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);
        ReflectionTestUtils.setField(book, "id", 1L);
        Book otherBook = new Book(new BookTitle("다른 제목"), null, null, 1, null);
        ReflectionTestUtils.setField(otherBook, "id", 2L);
        Chapter chapter = new Chapter(book, new ChapterTitle("1장"), 1);

        assertThat(chapter.belongsTo(otherBook)).isFalse();
    }

    @Test
    @DisplayName("목차 제목이 공백이거나 100자를 넘으면 거부한다")
    void rejectInvalidTitle() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);

        assertThatThrownBy(() -> new Chapter(book, new ChapterTitle(" "), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Chapter(book, new ChapterTitle("가".repeat(101)), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
