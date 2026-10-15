package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.domain.vo.SentenceContent;

class CommentedSentenceTest {

    @ParameterizedTest
    @CsvSource({"1, false", "2, false", "3, true"})
    void isFutureOf(int passageSequence, boolean expected) {
        Passage currentPassage = new Passage(null, 2, List.of(new SentenceContent("현재 문장")));
        CommentedSentence sentence = new CommentedSentence(
                1L, "댓글이 있는 문장", 1L,
                new ContentSequence(passageSequence), new ContentSequence(1),
                1, 1, LocalDateTime.of(2026, 10, 15, 10, 0));

        assertThat(sentence.isFutureOf(currentPassage)).isEqualTo(expected);
    }
}
