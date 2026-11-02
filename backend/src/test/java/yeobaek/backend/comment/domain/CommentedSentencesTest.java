package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.book.domain.vo.ContentSequence;

class CommentedSentencesTest {

    @Test
    @DisplayName("ALL 필터는 모든 문장을 원래 순서대로 유지한다")
    void allKeepsSentencesInOriginalOrder() {
        CommentedSentence first = sentence(1L, 1, 1, 0);
        CommentedSentence second = sentence(2L, 1, 2, 1);
        CommentedSentence last = sentence(3L, 2, 1, 1);
        CommentedSentences sentences = new CommentedSentences(List.of(last, second, first));

        assertThat(sentences.filteredBy(CommentedSentenceFilter.ALL).values()).containsExactly(last, second, first);
    }

    @Test
    @DisplayName("NEW 필터는 원본을 바꾸지 않고 미확인 댓글 문장만 원래 순서대로 선택한다")
    void newSelectsUnreadSentencesWithoutChangingOriginal() {
        CommentedSentence read = sentence(1L, 1, 1, 0);
        CommentedSentence unread = sentence(2L, 1, 2, 1);
        CommentedSentence future = sentence(3L, 10, 1, 2);
        CommentedSentences sentences = new CommentedSentences(List.of(future, read, unread));

        assertThat(sentences.filteredBy(CommentedSentenceFilter.NEW).values()).containsExactly(future, unread);
        assertThat(sentences.values()).containsExactly(future, read, unread);
    }

    @Test
    @DisplayName("책 순서 정렬은 확인 여부와 작성 시각에 관계없이 문단과 문장 순서를 따른다")
    void sortsByPassageThenSentence() {
        CommentedSentence first = sentence(1L, 1, 1, 0);
        CommentedSentence second = sentence(2L, 1, 2, 1);
        CommentedSentence last = sentence(3L, 2, 1, 1);

        assertThat(new CommentedSentences(List.of(last, second, first)).sortedInBookOrder())
                .containsExactly(first, second, last);
    }

    @Test
    @DisplayName("책의 위치가 같은 문장은 문장 ID가 큰 순서로 보여준다")
    void ordersEqualPositionsByDescendingSentenceId() {
        CommentedSentence lowerId = sentence(1L, 1, 1, 1);
        CommentedSentence higherId = sentence(2L, 1, 1, 1);

        assertThat(new CommentedSentences(List.of(lowerId, higherId)).sortedInBookOrder())
                .containsExactly(higherId, lowerId);
    }

    @Test
    @DisplayName("빈 목록을 정렬하면 빈 목록을 반환한다")
    void sortsEmptyList() {
        assertThat(new CommentedSentences(List.of()).sortedInBookOrder()).isEmpty();
    }

    private CommentedSentence sentence(Long id, int passageSequence, int sentenceSequence, long unreadCount) {
        return new CommentedSentence(id, "문장 " + id, (long) passageSequence,
                new ContentSequence(passageSequence), new ContentSequence(sentenceSequence),
                2L, unreadCount, LocalDateTime.of(2026, 10, 1, 10, 0).plusDays(id));
    }
}
