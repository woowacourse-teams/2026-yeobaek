package yeobaek.backend.foundation.identity;

public record ContentLocationId(Long value) {

    public ContentLocationId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("컨텐츠 위치 식별자는 양수여야 합니다.");
        }
    }
}
