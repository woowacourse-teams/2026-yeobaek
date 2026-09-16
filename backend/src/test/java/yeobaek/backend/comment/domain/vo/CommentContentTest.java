package yeobaek.backend.comment.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentContentTest {

    @Test
    @DisplayName("댓글 내용은 값으로 동등성을 비교하고 공백을 보존한다")
    void hasValueEqualityAndPreservesWhitespace() {
        CommentContent content = new CommentContent("  댓글  ");

        assertThat(content).isEqualTo(new CommentContent("  댓글  "));
        assertThat(content.value()).isEqualTo("  댓글  ");
    }
}
