package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import yeobaek.backend.comment.domain.vo.CommentContent;

class CommentTest {

    @Test
    @DisplayName("1~1000자 내용으로 댓글을 생성할 수 있다")
    void createWithValidContent() {
        assertThatCode(() -> new Comment(1L, new CommentContent("가".repeat(1000))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("댓글 본문은 감상 루트 ID를 공유한다")
    void sharesAppreciationRootId() {
        Comment comment = new Comment(1L, new CommentContent("내용"));

        assertThat(comment.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("댓글 본문의 내용을 수정한다")
    void updateContent() {
        Comment comment = new Comment(1L, new CommentContent("원본"));

        comment.updateContent(new CommentContent("수정된 내용"));

        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("내용이 없거나 공백뿐이면 댓글 생성에 실패한다")
    void rejectBlankContent(String content) {
        assertThatThrownBy(() -> new Comment(1L, new CommentContent(content)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("내용이 1000자를 넘으면 댓글 생성에 실패한다")
    void rejectTooLongContent() {
        assertThatThrownBy(() -> new Comment(1L, new CommentContent("가".repeat(1001))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
