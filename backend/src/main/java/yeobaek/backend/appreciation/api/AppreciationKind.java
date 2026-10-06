package yeobaek.backend.appreciation.api;

public record AppreciationKind(String value) {

    public static final AppreciationKind COMMENT = new AppreciationKind("COMMENT");

    public AppreciationKind {
        if (value == null) {
            throw new IllegalArgumentException("감상 종류는 null일 수 없습니다.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
