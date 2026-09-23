package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.vo.BookDeduplicationKey;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.Publisher;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

class BookTest {

    private static final String COVER_KEY = "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.webp";

    @Test
    @DisplayName("새 도서는 ACTIVE 상태이다")
    void newBookIsActive() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);

        assertThat(book.getStatus()).isEqualTo(BookStatus.ACTIVE);
    }

    @Test
    @DisplayName("도서를 삭제하면 DELETED 상태로 전이한다")
    void transitionsToDeleted() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);

        book.delete();

        assertThat(book.getStatus()).isEqualTo(BookStatus.DELETED);
    }

    @Test
    @DisplayName("삭제된 도서는 다시 삭제할 수 없다")
    void cannotDeleteTwice() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);
        book.delete();

        assertThatThrownBy(book::delete)
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("같은 id를 가진 도서는 동일한 도서로 판단한다")
    void isSameWhenIdMatches() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);
        Book other = new Book(new BookTitle("다른 제목"), null, null, 2, null);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(other, "id", 1L);

        assertThat(book.isSame(other)).isTrue();
    }

    @Test
    @DisplayName("id가 다른 도서는 동일한 도서가 아니라고 판단한다")
    void isNotSameWhenIdDiffers() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);
        Book other = new Book(new BookTitle("제목"), null, null, 1, null);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(other, "id", 2L);

        assertThat(book.isSame(other)).isFalse();
    }

    @Test
    @DisplayName("도서의 중복 판단 기준을 제목·출판사·출판연도·작가로 구성한다")
    void deduplicationKey() {
        Book book = new Book(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, 1, null);

        assertThat(book.deduplicationKey(Set.of(1L, 2L)))
                .isEqualTo(new BookDeduplicationKey(
                        new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924,
                        Set.of(1L, 2L)));
    }

    @Test
    @DisplayName("제목이 공백이거나 100자를 넘으면 거부한다")
    void rejectInvalidTitle() {
        assertThatThrownBy(() -> new Book(new BookTitle(" "), null, null, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Book(new BookTitle("가".repeat(101)), null, null, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("출판사가 공백이거나 100자를 넘으면 거부한다")
    void rejectInvalidPublisher() {
        assertThatThrownBy(() -> new Book(new BookTitle("제목"), new Publisher(" "), null, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Book(new BookTitle("제목"), new Publisher("가".repeat(101)), null, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("표지 이미지 키는 없을 수 있고 prefix를 포함한 UUID 키 형식만 허용한다")
    void validateCoverImageKey() {
        assertThat(new Book(new BookTitle("제목"), null, null, 1, null).getCoverImageKey()).isNull();
        assertThat(new Book(new BookTitle("제목"), null, null, 1, COVER_KEY).getCoverImageKey()).isEqualTo(COVER_KEY);
        assertThatThrownBy(() -> new Book(new BookTitle("제목"), null, null, 1, "book-covers/123e4567-e89b-12d3-a456-426614174000.webp"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("활성 도서의 표지를 교체하고 제거할 수 있다")
    void replaceAndRemoveCoverImage() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null);

        book.replaceCoverImage(COVER_KEY);
        assertThat(book.getCoverImageKey()).isEqualTo(COVER_KEY);

        book.removeCoverImage();
        assertThat(book.getCoverImageKey()).isNull();
    }
}
