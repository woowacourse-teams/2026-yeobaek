package yeobaek.backend.foundation.identity;

public record MemberId(Long value) {

    public MemberId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("회원 식별자는 양수여야 합니다.");
        }
    }
}
