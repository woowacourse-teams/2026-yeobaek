package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class PassageTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("문장 내용이 없거나 공백뿐이면 문단 생성에 실패한다")
    void rejectBlankContent(String content) {
        Book book = new Book("제목", null, null, 1);
        Chapter chapter = new Chapter(book, "1장", 1);

        assertThatThrownBy(() -> new Passage(chapter, 1, content))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("문장이 없으면 문단 생성에 실패한다")
    void rejectEmptySentences() {
        Book book = new Book("제목", null, null, 1);
        Chapter chapter = new Chapter(book, "1장", 1);

        assertThatThrownBy(() -> new Passage(chapter, 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("문장은 배열 순서대로 dense sequence를 가지며 원문의 공백과 개행을 보존한다")
    void createSentencesInDenseOrderWithoutTrimming() {
        Book book = new Book("제목", null, null, 1);
        Chapter chapter = new Chapter(book, "1장", 1);

        Passage passage = new Passage(chapter, 1, List.of("첫 문장.  ", "둘째 문장.\n"));

        assertThat(passage.getSentences()).extracting(Sentence::getSequence).containsExactly(1, 2);
        assertThat(passage.getSentences()).extracting(Sentence::getContent)
                .containsExactly("첫 문장.  ", "둘째 문장.\n");
        assertThatThrownBy(() -> passage.getSentences().add(passage.getSentences().getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("본문이 속한 챕터의 도서와 같은 도서를 전달하면 참을 반환한다")
    void belongsToOwnBook() {
        Book book = new Book("제목", null, null, 1);
        ReflectionTestUtils.setField(book, "id", 1L);
        Chapter chapter = new Chapter(book, "1장", 1);
        Passage passage = new Passage(chapter, 1, "본문");

        assertThat(passage.belongsTo(book)).isTrue();
    }

    @Test
    @DisplayName("본문이 속하지 않은 도서를 전달하면 거짓을 반환한다")
    void doesNotBelongToOtherBook() {
        Book book = new Book("제목", null, null, 1);
        ReflectionTestUtils.setField(book, "id", 1L);
        Book otherBook = new Book("다른 제목", null, null, 1);
        ReflectionTestUtils.setField(otherBook, "id", 2L);
        Chapter chapter = new Chapter(book, "1장", 1);
        Passage passage = new Passage(chapter, 1, "본문");

        assertThat(passage.belongsTo(otherBook)).isFalse();
    }
}
