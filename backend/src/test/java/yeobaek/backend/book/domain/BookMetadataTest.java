package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.support.BadRequestException;

class BookMetadataTest {

    @Test
    @DisplayName("자유 장르와 다섯 개·열 문자 경계의 태그를 보존하고 외부 변경을 막는다")
    void preservesMetadataAndProtectsCollections() {
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
    void replacesMetadataIndependently() {
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
