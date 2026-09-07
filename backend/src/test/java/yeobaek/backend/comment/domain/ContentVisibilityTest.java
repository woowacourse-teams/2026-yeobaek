package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ContentVisibilityTest {

    @ParameterizedTest
    @CsvSource({
            "true, 1, REVEAL_REQUIRED",
            "true, 0, VISIBLE",
            "false, 1, VISIBLE",
            "false, 0, VISIBLE"
    })
    @DisplayName("미래 문장에 안 읽은 댓글이 있을 때만 내용을 가린다")
    void determineContentVisibility(boolean future, long unreadCommentCount, ContentVisibility expected) {
        assertThat(ContentVisibility.from(future, unreadCommentCount)).isEqualTo(expected);
    }
}
