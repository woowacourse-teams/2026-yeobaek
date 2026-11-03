package yeobaek.backend.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import yeobaek.backend.book.domain.vo.BookGenres;
import yeobaek.backend.book.domain.vo.BookHashtags;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.support.BadRequestException;

class BookMetadataTest {

    @Test
    @DisplayName("장르와 태그는 목록 전체로 독립 교체한다")
    void replacesMetadataIndependently() {
        Book book = new Book(new BookTitle("책"), null, null, 1, null, null, null,
                new BookGenres(List.of("비극")), new BookHashtags(List.of("첫사랑")));

        book.replaceGenres(new BookGenres(List.of("로맨스", "비극")));
        assertThat(book).extracting(Book::genreValues, Book::hashtagValues)
                .containsExactly(List.of("로맨스", "비극"), List.of("첫사랑"));

        book.replaceHashtags(new BookHashtags(List.of("재회", "오해")));
        assertThat(book).extracting(Book::genreValues, Book::hashtagValues)
                .containsExactly(List.of("로맨스", "비극"), List.of("재회", "오해"));
    }

    @ParameterizedTest
    @MethodSource("emptyGenres")
    @DisplayName("null·빈 장르 저장은 장르만 제거한다")
    void clearsGenres(BookGenres genres) {
        Book book = new Book(new BookTitle("책"), null, null, 1, null, null, null,
                new BookGenres(List.of("비극")), new BookHashtags(List.of("첫사랑")));

        book.replaceGenres(genres);

        assertThat(book).extracting(Book::getGenres, Book::genreValues, Book::hashtagValues)
                .containsExactly(null, List.of(), List.of("첫사랑"));
    }

    @ParameterizedTest
    @MethodSource("emptyHashtags")
    @DisplayName("null·빈 태그 저장은 태그만 제거한다")
    void clearsHashtags(BookHashtags hashtags) {
        Book book = new Book(new BookTitle("책"), null, null, 1, null, null, null,
                new BookGenres(List.of("비극")), new BookHashtags(List.of("첫사랑")));

        book.replaceHashtags(hashtags);

        assertThat(book).extracting(Book::genreValues, Book::getHashtags, Book::hashtagValues)
                .containsExactly(List.of("비극"), null, List.of());
    }

    @Test
    @DisplayName("삭제된 도서는 장르·태그를 바꾸지 않는다")
    void rejectsDeletedBookChanges() {
        Book book = new Book(new BookTitle("책"), null, null, 1, null, null, null);
        book.delete();

        assertThatThrownBy(() -> book.replaceGenres(new BookGenres(List.of("비극"))))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> book.replaceHashtags(new BookHashtags(List.of("첫사랑"))))
                .isInstanceOf(BadRequestException.class);
    }

    private static Stream<BookGenres> emptyGenres() {
        return Stream.of(null, new BookGenres(List.of()));
    }

    private static Stream<BookHashtags> emptyHashtags() {
        return Stream.of(null, new BookHashtags(List.of()));
    }
}
