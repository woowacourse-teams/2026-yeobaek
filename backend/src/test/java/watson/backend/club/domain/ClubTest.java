package watson.backend.club.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import watson.backend.book.domain.Book;
import watson.backend.book.domain.Chapter;
import watson.backend.book.domain.Passage;

class ClubTest {

    @Test
    @DisplayName("모임의 도서에 속한 본문이면 읽는 중이라고 판단한다")
    void isReadingOwnBookPassage() {
        Book book = new Book("제목", null, null, 1);
        ReflectionTestUtils.setField(book, "id", 1L);
        Chapter chapter = new Chapter(book, "1장", 1);
        Passage passage = new Passage(chapter, 1, "본문", null);
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
        Passage otherPassage = new Passage(otherChapter, 1, "다른 본문", null);
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
