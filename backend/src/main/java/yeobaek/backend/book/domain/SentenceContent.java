package yeobaek.backend.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record SentenceContent(@Column(name = "value", columnDefinition = "TEXT", nullable = false) String value) {

    public SentenceContent {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("문장 내용은 공백이 아닌 텍스트여야 합니다.");
        }
    }
}
