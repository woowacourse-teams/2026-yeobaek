package yeobaek.backend.appreciation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

class CommentCoreTest {

    @Test
    @DisplayName("댓글은 본문 변경과 수정 시각을 자신의 상태 변화로 함께 처리한다")
    void updateOwnState() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 1, 12, 0);
        var comment = new Comment(new AppreciationId(1L), new MemberId(2L), "처음", createdAt, null);
        Clock clock = Clock.fixed(Instant.parse("2026-10-02T03:04:05Z"), ZoneOffset.UTC);

        comment.update("수정", clock);

        assertThat(comment.content()).isEqualTo("수정");
        assertThat(comment.createdAt()).isEqualTo(createdAt);
        assertThat(comment.updatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 3, 4, 5));
        assertThat(comment.isWrittenBy(new MemberId(2L))).isTrue();
    }
}
