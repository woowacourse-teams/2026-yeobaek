package watson.backend.comment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CommentTest {

    @Test
    @DisplayName("1~1000자 내용으로 댓글을 생성할 수 있다")
    void createWithValidContent() {
        assertThatCode(() -> new Comment(null, null, "가".repeat(1000)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("내용이 없거나 공백뿐이면 댓글 생성에 실패한다")
    void rejectBlankContent(String content) {
        assertThatThrownBy(() -> new Comment(null, null, content))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("내용이 1000자를 넘으면 댓글 생성에 실패한다")
    void rejectTooLongContent() {
        assertThatThrownBy(() -> new Comment(null, null, "가".repeat(1001)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
