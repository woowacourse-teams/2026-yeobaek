package yeobaek.backend.comment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record CommentContent(@Column(name = "value", nullable = false, length = MAX_LENGTH) String value) {

    static final int MAX_LENGTH = 1000;

    public CommentContent {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("댓글 내용은 공백이 아닌 1~" + MAX_LENGTH + "자여야 합니다.");
        }
    }
}
