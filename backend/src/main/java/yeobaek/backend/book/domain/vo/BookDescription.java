package yeobaek.backend.book.domain.vo;

import jakarta.persistence.Embeddable;

@Embeddable
public record BookDescription(String value) {

    public BookDescription {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("책 소개는 공백일 수 없습니다.");
        }
        value = value.strip();
    }
}
