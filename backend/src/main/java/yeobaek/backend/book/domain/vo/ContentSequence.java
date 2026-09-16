package yeobaek.backend.book.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ContentSequence(@Column(name = "value", nullable = false) int value) {

    private static final int FIRST_SEQUENCE = 1;

    public ContentSequence {
        if (value < FIRST_SEQUENCE) {
            throw new IllegalArgumentException("콘텐츠 순서는 1 이상이어야 합니다.");
        }
    }
}
