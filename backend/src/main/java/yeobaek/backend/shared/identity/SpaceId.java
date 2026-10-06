package yeobaek.backend.shared.identity;

public record SpaceId(Long value) {

    public SpaceId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("공간 식별자는 양수여야 합니다.");
        }
    }
}
