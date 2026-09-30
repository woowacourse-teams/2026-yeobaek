package yeobaek.backend.publicroom.dto;

public enum PublicRoomSort {
    MOST_VISITED;

    public static PublicRoomSort from(String value) {
        if (value == null) {
            return MOST_VISITED;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("공개방 정렬 값은 비어 있을 수 없습니다.");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 공개방 정렬입니다: " + value, exception);
        }
    }
}
