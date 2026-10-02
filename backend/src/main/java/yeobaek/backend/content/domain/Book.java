package yeobaek.backend.content.domain;

import yeobaek.backend.foundation.identity.ContentId;

public record Book(ContentId id, Long bookId, boolean available, int passageCount) implements Content {

    public static final String KIND = "BOOK";
    private static final int MIN_PASSAGE_COUNT = 1;

    public Book {
        if (id == null || bookId == null) {
            throw new IllegalArgumentException("도서의 필수 정보가 누락되었습니다.");
        }
        if (passageCount < MIN_PASSAGE_COUNT) {
            throw new IllegalArgumentException("도서의 본문 개수는 1개 이상이어야 합니다.");
        }
    }

    @Override
    public String kind() {
        return KIND;
    }
}
