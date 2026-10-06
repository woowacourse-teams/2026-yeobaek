package yeobaek.backend.space.api;

public record SpaceKind(String value) {

    public static final SpaceKind CLUB = new SpaceKind("CLUB");
    public static final SpaceKind PUBLIC_ROOM = new SpaceKind("PUBLIC_ROOM");

    public SpaceKind {
        if (value == null) {
            throw new IllegalArgumentException("공간 종류는 null일 수 없습니다.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
