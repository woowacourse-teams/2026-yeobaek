package yeobaek.backend.foundation.identity;

public record ContentId(Long value) {

    public ContentId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("컨텐츠 식별자는 양수여야 합니다.");
        }
    }
}
