package yeobaek.backend.foundation.identity;

public record AppreciationId(Long value) {

    public AppreciationId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("감상 식별자는 양수여야 합니다.");
        }
    }
}
