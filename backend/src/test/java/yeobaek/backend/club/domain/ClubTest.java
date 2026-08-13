package yeobaek.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.Chapter;
import yeobaek.backend.book.domain.Passage;

class ClubTest {

    @Test
    @DisplayName("1~20자 이름으로 모임을 생성할 수 있다")
    void createWithValidName() {
        Book book = new Book("제목", null, null, 1);

        assertThatCode(() -> new Club("모", book, "CODE01"))
                .doesNotThrowAnyException();
        assertThat(new Club("가".repeat(20), book, "CODE01").getName()).hasSize(20);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("이름이 없거나 공백뿐이면 모임 생성에 실패한다")
    void rejectBlankName(String name) {
        Book book = new Book("제목", null, null, 1);

        assertThatThrownBy(() -> new Club(name, book, "CODE01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이름이 20자를 넘으면 모임 생성에 실패한다")
    void rejectTooLongName() {
        Book book = new Book("제목", null, null, 1);

        assertThatThrownBy(() -> new Club("가".repeat(21), book, "CODE01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("모임의 도서에 속한 본문이면 읽는 중이라고 판단한다")
    void isReadingOwnBookPassage() {
        Book book = new Book("제목", null, null, 1);
        ReflectionTestUtils.setField(book, "id", 1L);
        Chapter chapter = new Chapter(book, "1장", 1);
        Passage passage = new Passage(chapter, 1, "본문");
        Club club = new Club("1기", book, "CODE01");

        assertThat(club.isReading(passage)).isTrue();
    }

    @Test
    @DisplayName("모임의 도서에 속하지 않은 본문이면 읽는 중이 아니라고 판단한다")
    void isNotReadingOtherBookPassage() {
        Book book = new Book("제목", null, null, 1);
        ReflectionTestUtils.setField(book, "id", 1L);
        Book otherBook = new Book("다른 제목", null, null, 1);
        ReflectionTestUtils.setField(otherBook, "id", 2L);
        Chapter otherChapter = new Chapter(otherBook, "1장", 1);
        Passage otherPassage = new Passage(otherChapter, 1, "다른 본문");
        Club club = new Club("1기", book, "CODE01");

        assertThat(club.isReading(otherPassage)).isFalse();
    }

    @Test
    @DisplayName("모임의 총 본문 개수는 도서의 본문 개수와 같다")
    void totalPassageCountDelegatesToBook() {
        Book book = new Book("제목", null, null, 7);
        Club club = new Club("1기", book, "CODE01");

        assertThat(club.totalPassageCount()).isEqualTo(7);
    }
}
