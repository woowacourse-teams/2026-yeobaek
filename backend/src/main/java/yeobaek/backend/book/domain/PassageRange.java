package yeobaek.backend.book.domain;

public record PassageRange(int from, int to) {

    private static final int FIRST_SEQUENCE = 1;

    public PassageRange {
        if (from < FIRST_SEQUENCE || to < from) {
            throw new IllegalArgumentException("본문 범위가 올바르지 않습니다.");
        }
    }

    public int size() {
        return to - from + 1;
    }
}
