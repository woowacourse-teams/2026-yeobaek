package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.book.domain.vo.ContentSequence;

class CommentedSentenceFilterTest {

    @Test
    @DisplayName("ALL은 미확인 댓글이 있든 없든 문장을 포함한다")
    void allMatchesRegardlessOfUnreadComments() {
        assertThat(CommentedSentenceFilter.ALL.matches(sentence(0))).isTrue();
        assertThat(CommentedSentenceFilter.ALL.matches(sentence(1))).isTrue();
    }

    @Test
    @DisplayName("NEW는 댓글을 모두 확인한 문장을 제외한다")
    void newExcludesFullyReadSentence() {
        assertThat(CommentedSentenceFilter.NEW.matches(sentence(0))).isFalse();
    }

    @Test
    @DisplayName("NEW는 미확인 댓글이 하나라도 있는 문장을 포함한다")
    void newMatchesSentenceWithUnreadComment() {
        assertThat(CommentedSentenceFilter.NEW.matches(sentence(1))).isTrue();
    }

    private CommentedSentence sentence(long unreadCommentCount) {
        return new CommentedSentence(1L, "댓글이 있는 문장", 1L,
                new ContentSequence(1), new ContentSequence(1),
                2L, unreadCommentCount, LocalDateTime.of(2026, 10, 1, 10, 0));
    }
}
