package yeobaek.backend.content.api;

public record ContentKind(String value) {

    public static final ContentKind BOOK = new ContentKind("BOOK");

    public ContentKind {
        if (value == null) {
            throw new IllegalArgumentException("컨텐츠 종류는 null일 수 없습니다.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
