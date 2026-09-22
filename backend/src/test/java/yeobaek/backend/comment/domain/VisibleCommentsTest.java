package yeobaek.backend.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class VisibleCommentsTest {

    @Test
    @DisplayName("보이는 댓글 id를 순서대로 반환한다")
    void ids() {
        Comment first = commentWithId(1L);
        Comment second = commentWithId(2L);
        VisibleComments comments = new VisibleComments(List.of(first, second));

        assertThat(comments.ids()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("제외할 id에 포함되지 않은 댓글만 반환한다")
    void excludingIds() {
        Comment first = commentWithId(1L);
        Comment second = commentWithId(2L);
        VisibleComments comments = new VisibleComments(List.of(first, second));

        assertThat(comments.excludingIds(Set.of(1L))).containsExactly(second);
    }

    @Test
    @DisplayName("댓글이 없으면 빈 컬렉션이다")
    void isEmpty() {
        VisibleComments comments = new VisibleComments(List.of());

        assertThat(comments.isEmpty()).isTrue();
    }

    private Comment commentWithId(Long id) {
        Comment comment = new Comment();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
