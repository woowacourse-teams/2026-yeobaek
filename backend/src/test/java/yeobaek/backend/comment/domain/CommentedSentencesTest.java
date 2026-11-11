package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.domain.vo.ContentSequence;
import yeobaek.backend.book.domain.vo.SentenceContent;

class CommentedSentencesTest {

    @Test
    @DisplayName("현재 문단까지 미확인 댓글이 있는 문장만 원본 순서대로 고른다")
    void selectsUnreadSentencesThroughCurrentPassage() {
        Passage currentPassage = new Passage(null, 2, List.of(new SentenceContent("현재 문장")));
        CommentedSentence read = sentence(1L, 1, 1, 0);
        CommentedSentence unread = sentence(2L, 2, 2, 1);
        CommentedSentence future = sentence(3L, 10, 1, 2);
        CommentedSentence earlierUnread = sentence(4L, 1, 2, 1);
        CommentedSentences sentences = new CommentedSentences(List.of(future, read, unread, earlierUnread));

        assertThat(sentences.withUnreadCommentsUpTo(currentPassage).values())
                .containsExactly(unread, earlierUnread);
        assertThat(sentences.values()).containsExactly(future, read, unread, earlierUnread);
    }

    @Test
    @DisplayName("책 순서 정렬은 확인 여부와 작성 시각에 관계없이 문단과 문장 순서를 따른다")
    void sortsByPassageThenSentence() {
        CommentedSentence first = sentence(1L, 1, 1, 0);
        CommentedSentence second = sentence(2L, 1, 2, 1);
        CommentedSentence last = sentence(3L, 2, 1, 1);

        assertThat(new CommentedSentences(List.of(last, second, first)).sortedByPositionInBook())
                .containsExactly(first, second, last);
    }

    @Test
    @DisplayName("빈 목록을 정렬하면 빈 목록을 반환한다")
    void sortsEmptyList() {
        assertThat(new CommentedSentences(List.of()).sortedByPositionInBook()).isEmpty();
    }

    private CommentedSentence sentence(Long id, int passageSequence, int sentenceSequence, long unreadCount) {
        return new CommentedSentence(id, "문장 " + id, (long) passageSequence,
                new ContentSequence(passageSequence), new ContentSequence(sentenceSequence),
                2L, unreadCount, LocalDateTime.of(2026, 10, 1, 10, 0).plusDays(id));
    }
}
