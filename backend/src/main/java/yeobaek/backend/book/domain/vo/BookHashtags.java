package yeobaek.backend.book.domain.vo;

import java.util.List;

public record BookHashtags(List<String> value) {

    public static final int MAX_COUNT = 5;
    public static final int MAX_LENGTH = 10;

    public BookHashtags {
        if (value == null) {
            throw new IllegalArgumentException("해시태그 목록은 null일 수 없습니다.");
        }
        if (value.size() > MAX_COUNT) {
            throw new IllegalArgumentException("해시태그는 최대 5개까지 등록할 수 있습니다.");
        }
        for (String hashtag : value) {
            if (hashtag == null || hashtag.codePointCount(0, hashtag.length()) > MAX_LENGTH) {
                throw new IllegalArgumentException("해시태그는 각각 최대 10자까지 등록할 수 있습니다.");
            }
        }
        value = List.copyOf(value);
    }
}
