package yeobaek.backend.book.domain.vo;

import java.util.Objects;
import java.util.Set;

public record BookDuplicateCriteria(
        BookTitle title,
        Publisher publisher,
        Integer publishedYear,
        Set<Long> authorIds
) {

    public BookDuplicateCriteria {
        Objects.requireNonNull(title, "도서 제목은 필수입니다.");
        authorIds = Set.copyOf(Objects.requireNonNull(authorIds, "작가 목록은 필수입니다."));
    }

    public boolean isDuplicateOf(BookDuplicateCriteria other) {
        return equals(other);
    }
}
