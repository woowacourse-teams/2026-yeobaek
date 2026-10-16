package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.domain.vo.SentenceContent;

class CommentedSentenceTest {

    @Test
    void rejectEmptyComments() {
        assertThatThrownBy(() -> CommentedSentence.from(new Comments(List.of()), Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글이 없어 대표 댓글을 선택할 수 없습니다.");
    }

    @Test
    void summarizeScalarCommentsBySentence() {
        LocalDateTime firstCreatedAt = LocalDateTime.of(2026, 10, 15, 10, 0);
        LocalDateTime latestCreatedAt = firstCreatedAt.plusHours(1);
        var comments = List.of(
                new TestComment(1L, firstCreatedAt, 10L, "첫 문장", 100L, 1, 1),
                new TestComment(2L, latestCreatedAt, 10L, "첫 문장", 100L, 1, 1),
                new TestComment(3L, firstCreatedAt, 20L, "둘째 문장", 100L, 1, 2));

        List<CommentedSentence> summaries = CommentedSentence.summarize(comments, Set.of(1L));

        assertThat(summaries).containsExactly(
                new CommentedSentence(
                        10L, "첫 문장", 100L,
                        new ContentSequence(1), new ContentSequence(1),
                        2, 1, latestCreatedAt),
                new CommentedSentence(
                        20L, "둘째 문장", 100L,
                        new ContentSequence(1), new ContentSequence(2),
                        1, 1, firstCreatedAt));
    }

    @Test
    void summarizeEmptyScalarComments() {
        assertThat(CommentedSentence.summarize(List.of(), Set.of())).isEmpty();
    }

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

    private record TestComment(
            Long commentId,
            LocalDateTime createdAt,
            Long sentenceId,
            String sentenceContent,
            Long passageId,
            int passageSequence,
            int sentenceSequence
    ) implements CommentedSentence.CommentData {
    }
}
