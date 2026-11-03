package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.test.util.ReflectionTestUtils;
import yeobaek.backend.book.domain.vo.BookDeduplicationKey;
import yeobaek.backend.book.domain.vo.BookDescription;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.book.domain.vo.Publisher;
import yeobaek.backend.support.BadRequestException;
import yeobaek.backend.support.ErrorCode;

class BookTest {

    private static final String COVER_KEY = "yeobaek/book-covers/123e4567-e89b-12d3-a456-426614174000.webp";

    @Test
    @DisplayName("새 도서는 ACTIVE 상태이다")
    void newBookIsActive() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, null, null, null, null);

        assertThat(book.getStatus()).isEqualTo(BookStatus.ACTIVE);
    }

    @Test
    @DisplayName("도서를 삭제하면 DELETED 상태로 전이한다")
    void transitionsToDeleted() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, null, null, null, null);

        book.delete();

        assertThat(book.getStatus()).isEqualTo(BookStatus.DELETED);
    }

    @Test
    @DisplayName("삭제된 도서는 다시 삭제할 수 없다")
    void cannotDeleteTwice() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, null, null, null, null);
        book.delete();

        assertThatThrownBy(book::delete)
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("삭제된 도서의 소개는 교체할 수 없다")
    void cannotReplaceDescriptionOfDeletedBook() {
        var description = new BookDescription("기존 소개");
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, description, null, null, null);
        book.delete();

        assertThatThrownBy(() -> book.replaceDescription(new BookDescription("새 소개")))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
        assertThat(book.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("교체할 소개가 null이면 기존 소개를 유지하고 거부한다")
    void rejectNullDescriptionOnReplacement() {
        var description = new BookDescription("기존 소개");
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, description, null, null, null);

        assertThatThrownBy(() -> book.replaceDescription(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(book.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("삭제된 도서의 소개는 제거할 수 없다")
    void cannotRemoveDescriptionOfDeletedBook() {
        var description = new BookDescription("기존 소개");
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, description, null, null, null);
        book.delete();

        assertThatThrownBy(book::removeDescription)
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
        assertThat(book.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("이용할 수 없는 저장 도서는 예외 로그 컨텍스트에 도서 ID와 상태를 제공한다")
    void unavailableBookHasLogContext() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", 3L);
        book.delete();

        assertThatThrownBy(book::ensureAvailable)
                .isInstanceOf(BadRequestException.class)
                .extracting("logContext")
                .isEqualTo(Map.of("bookId", "3", "bookStatus", "DELETED"));
    }

    @Test
    @DisplayName("같은 id를 가진 도서는 동일한 도서로 판단한다")
    void isSameWhenIdMatches() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, null, null, null, null);
        Book other = new Book(new BookTitle("다른 제목"), null, null, 2, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(other, "id", 1L);

        assertThat(book.isSame(other)).isTrue();
    }

    @Test
    @DisplayName("id가 다른 도서는 동일한 도서가 아니라고 판단한다")
    void isNotSameWhenIdDiffers() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, null, null, null, null);
        Book other = new Book(new BookTitle("제목"), null, null, 1, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", 1L);
        ReflectionTestUtils.setField(other, "id", 2L);

        assertThat(book.isSame(other)).isFalse();
    }

    @Test
    @DisplayName("도서의 중복 판단 기준을 제목·출판사·출판연도·작가로 구성한다")
    void deduplicationKey() {
        Book book = new Book(new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924, 1, null, null, null, null, null);

        assertThat(book.deduplicationKey(Set.of(1L, 2L)))
                .isEqualTo(new BookDeduplicationKey(
                        new BookTitle("운수 좋은 날"), new Publisher("자체 제작"), 1924,
                        Set.of(1L, 2L)));
    }

    @Test
    @DisplayName("제목이 공백이거나 100자를 넘으면 거부한다")
    void rejectInvalidTitle() {
        assertThatThrownBy(() -> new Book(new BookTitle(" "), null, null, 1, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Book(new BookTitle("가".repeat(101)), null, null, 1, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("출판사가 공백이거나 100자를 넘으면 거부한다")
    void rejectInvalidPublisher() {
        assertThatThrownBy(() -> new Book(new BookTitle("제목"), new Publisher(" "), null, 1, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Book(new BookTitle("제목"), new Publisher("가".repeat(101)), null, 1, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("표지 이미지 키는 없을 수 있고 prefix를 포함한 UUID 키 형식만 허용한다")
    void validateCoverImageKey() {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        assertThat(new Book(new BookTitle("제목"), null, null, 1, uuid, null, null, null, null).getCoverImageKey()).isEqualTo(uuid);
        assertThat(new Book(new BookTitle("제목"), null, null, 1, null, null, null, null, null).getCoverImageKey()).isNull();
        assertThat(new Book(new BookTitle("제목"), null, null, 1, COVER_KEY, null, null, null, null).getCoverImageKey()).isEqualTo(COVER_KEY);
        assertThatThrownBy(() -> new Book(new BookTitle("제목"), null, null, 1, "book-covers/123e4567-e89b-12d3-a456-426614174000.webp", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("활성 도서의 표지를 교체하고 제거할 수 있다")
    void replaceAndRemoveCoverImage() {
        Book book = new Book(new BookTitle("제목"), null, null, 1, null, null, null, null, null);

        book.replaceCoverImage(COVER_KEY);
        assertThat(book.getCoverImageKey()).isEqualTo(COVER_KEY);

        book.removeCoverImage();
        assertThat(book.getCoverImageKey()).isNull();
    }


    @Test
    @DisplayName("자유 장르와 다섯 개·열 문자 경계의 태그를 보존하고 외부 변경을 막는다")
    void preservesGenresAndHashtagsAndProtectsCollections() {
        List<String> expectedGenres = List.of("로맨스", "비극", "로맨스", " 자유 장르 ", "긴 장르".repeat(20), "성장");
        List<String> expectedTags = List.of("첫사랑", "#반전", "첫사랑", "가나다라마바사아자차", "😀".repeat(10));
        List<String> genres = new ArrayList<>(expectedGenres);
        List<String> tags = new ArrayList<>(expectedTags);
        Book book = book(genres, tags);

        genres.clear();
        tags.clear();

        assertThat(book).extracting(Book::genreValues, Book::hashtagValues)
                .containsExactly(expectedGenres, expectedTags);
        assertThatThrownBy(() -> book.getGenres().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> book.getHashtags().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("장르와 태그는 입력을 복사해 목록 전체로 독립 교체한다")
    void replacesGenresAndHashtagsIndependently() {
        Book book = book(List.of("비극"), List.of("첫사랑"));
        List<String> replacement = new ArrayList<>(List.of("로맨스", "비극"));

        book.replaceGenres(replacement);
        replacement.clear();
        assertThat(book).extracting(Book::genreValues, Book::hashtagValues)
                .containsExactly(List.of("로맨스", "비극"), List.of("첫사랑"));

        replacement.addAll(List.of("재회", "오해"));
        book.replaceHashtags(replacement);
        replacement.clear();
        assertThat(book).extracting(Book::genreValues, Book::hashtagValues)
                .containsExactly(List.of("로맨스", "비극"), List.of("재회", "오해"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null·빈 장르 저장은 장르만 제거한다")
    void clearsGenres(List<String> genres) {
        Book book = book(List.of("비극"), List.of("첫사랑"));

        book.replaceGenres(genres);

        assertThat(book).extracting(Book::genreValues, Book::hashtagValues)
                .containsExactly(List.of(), List.of("첫사랑"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null·빈 태그 저장은 태그만 제거한다")
    void clearsHashtags(List<String> hashtags) {
        Book book = book(List.of("비극"), List.of("첫사랑"));

        book.replaceHashtags(hashtags);

        assertThat(book).extracting(Book::genreValues, Book::hashtagValues)
                .containsExactly(List.of("비극"), List.of());
    }

    @ParameterizedTest
    @MethodSource("invalidHashtags")
    @DisplayName("태그 개수·문자 수·null 원소 규칙은 생성과 교체 모두에 적용하며 실패하면 기존 상태를 보존한다")
    void rejectsInvalidHashtagsWithoutChangingBook(List<String> invalid) {
        Book book = book(List.of("비극"), List.of("첫사랑"));

        assertThatThrownBy(() -> book(List.of("비극"), invalid)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> book.replaceHashtags(invalid)).isInstanceOf(IllegalArgumentException.class);
        assertThat(book).extracting(Book::genreValues, Book::hashtagValues)
                .containsExactly(List.of("비극"), List.of("첫사랑"));
    }

    @Test
    @DisplayName("null 장르 원소는 생성과 교체에서 거절하며 기존 장르를 지우지 않는다")
    void rejectsNullGenreWithoutChangingBook() {
        List<String> invalid = Arrays.asList("로맨스", null);
        Book book = book(List.of("비극"), List.of("첫사랑"));

        assertThatThrownBy(() -> book(invalid, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> book.replaceGenres(invalid)).isInstanceOf(IllegalArgumentException.class);
        assertThat(book.genreValues()).containsExactly("비극");
    }

    @Test
    @DisplayName("삭제된 도서는 장르·태그를 바꾸지 않는다")
    void rejectsDeletedBookChanges() {
        Book book = book(null, null);
        book.delete();

        assertThatThrownBy(() -> book.replaceGenres(List.of("비극"))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> book.replaceHashtags(List.of("첫사랑"))).isInstanceOf(BadRequestException.class);
    }

    private Book book(List<String> genres, List<String> hashtags) {
        return new Book(new BookTitle("책"), null, null, 1, null, null, null, genres, hashtags);
    }

    private static Stream<List<String>> invalidHashtags() {
        return Stream.of(List.of("가", "나", "다", "라", "마", "바"), List.of("가나다라마바사아자차카"),
                List.of("😀".repeat(11)), Arrays.asList("첫사랑", null));
    }
}
