package yeobaek.backend.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record PassageCount(@Column(name = "value", nullable = false) int value) {

    private static final int MINIMUM_COUNT = 1;

    public PassageCount {
        if (value < MINIMUM_COUNT) {
            throw new IllegalArgumentException("도서의 본문 개수는 1개 이상이어야 합니다.");
        }
    }
}
