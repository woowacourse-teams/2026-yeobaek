package yeobaek.backend.book.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ChapterTitle(@Column(name = "value", nullable = false, length = MAX_LENGTH) String value) {

    public static final int MAX_LENGTH = 100;

    public ChapterTitle {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("목차 제목은 공백이 아닌 1~" + MAX_LENGTH + "자여야 합니다.");
        }
    }
}
