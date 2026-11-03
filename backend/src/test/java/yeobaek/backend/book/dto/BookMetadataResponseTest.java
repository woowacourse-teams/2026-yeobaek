package yeobaek.backend.book.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.admin.dto.AdminBookResponse;
import yeobaek.backend.book.domain.Book;
import yeobaek.backend.book.domain.vo.BookGenres;
import yeobaek.backend.book.domain.vo.BookHashtags;
import yeobaek.backend.book.domain.vo.BookTitle;
import yeobaek.backend.club.dto.ClubBookResponse;

class BookMetadataResponseTest {

    @Test
    @DisplayName("도서·모임·관리자 응답은 같은 장르·태그 순서를 전달한다")
    void returnsStoredMetadata() {
        Book book = new Book(new BookTitle("책"), null, null, 1, null, null, null,
                new BookGenres(List.of("로맨스", "비극")), new BookHashtags(List.of("재회", "첫사랑")));

        var summary = BookSummaryResponse.of(book, List.of(), null);
        var detail = BookDetailResponse.of(book, List.of(), null, List.of());
        var club = ClubBookResponse.of(book, List.of(), null);
        var admin = AdminBookResponse.of(book, List.of(), null);

        assertThat(List.of(summary.genres(), detail.genres(), club.genres(), admin.genres()))
                .containsOnly(List.of("로맨스", "비극"));
        assertThat(List.of(summary.hashtags(), detail.hashtags(), club.hashtags(), admin.hashtags()))
                .containsOnly(List.of("재회", "첫사랑"));
    }

    @Test
    @DisplayName("과거 도서의 부재 정보는 모든 응답에서 빈 배열이다")
    void returnsEmptyMetadataForLegacyBook() {
        Book book = new Book(new BookTitle("기존 책"), null, null, 1, null, null, null);

        var summary = BookSummaryResponse.of(book, List.of(), null);
        var detail = BookDetailResponse.of(book, List.of(), null, List.of());
        var club = ClubBookResponse.of(book, List.of(), null);
        var admin = AdminBookResponse.of(book, List.of(), null);

        assertThat(List.of(summary.genres(), detail.genres(), club.genres(), admin.genres()))
                .containsOnly(List.of());
        assertThat(List.of(summary.hashtags(), detail.hashtags(), club.hashtags(), admin.hashtags()))
                .containsOnly(List.of());
    }
}
