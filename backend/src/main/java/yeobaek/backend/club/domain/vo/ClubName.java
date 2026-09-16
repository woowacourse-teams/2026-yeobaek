package yeobaek.backend.club.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ClubName(@Column(name = "value", nullable = false, length = MAX_LENGTH) String value) {

    public static final int MAX_LENGTH = 20;

    public ClubName {
        if (value == null || value.isBlank() || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("모임 이름은 공백이 아닌 1~" + MAX_LENGTH + "자여야 합니다.");
        }
    }
}
