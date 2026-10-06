package yeobaek.backend.content.api.location;

public record LocationKind(String value) {

    public static final LocationKind PASSAGE = new LocationKind("PASSAGE");
    public static final LocationKind SENTENCE = new LocationKind("SENTENCE");

    public LocationKind {
        if (value == null) {
            throw new IllegalArgumentException("컨텐츠 위치 종류는 null일 수 없습니다.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
