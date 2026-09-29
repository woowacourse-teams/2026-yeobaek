package yeobaek.backend.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import yeobaek.backend.book.repository.ActiveBookRepository;
import yeobaek.backend.book.repository.AuthorBookRepository;
import yeobaek.backend.book.repository.ChapterRepository;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.support.LogCapture;

class BookLoggingTest {

    @Test
    @DisplayName("검색어가 없더라도 도서 조회 로그에 keyword 필드를 null로 기록한다")
    void logNullKeyword() {
        ActiveBookRepository books = mock(ActiveBookRepository.class);
        when(books.findAll()).thenReturn(List.of());
        var service = new BookService(
                books,
                mock(AuthorBookRepository.class),
                mock(ChapterRepository.class),
                mock(PassageRepository.class),
                mock(BookCoverUrlResolver.class));

        try (var logs = new LogCapture(BookService.class.getName())) {
            service.findBooks(null);

            assertThat(logs.events()).hasSize(2);
            assertThat(logs.events()).allSatisfy(event -> assertThat(logs.hasField(event, "keyword")).isTrue());
            assertThat(logs.events()).allSatisfy(event -> assertThat(logs.field(event, "keyword")).isNull());
            assertThat(logs.events()).extracting(event -> logs.field(event, "operation"))
                    .containsOnly("book.findBooks");
            assertThat(logs.events()).extracting(event -> logs.field(event, "searchUsed"))
                    .containsOnly(false);
        }
    }

    @Test
    @DisplayName("검색어로 도서를 조회하면 모든 로그에 keyword와 searchUsed를 기록한다")
    void logSearchKeyword() {
        ActiveBookRepository books = mock(ActiveBookRepository.class);
        when(books.searchByTitleOrAuthorName("여백")).thenReturn(List.of());
        var service = new BookService(
                books,
                mock(AuthorBookRepository.class),
                mock(ChapterRepository.class),
                mock(PassageRepository.class),
                mock(BookCoverUrlResolver.class));

        try (var logs = new LogCapture(BookService.class.getName())) {
            service.findBooks("여백");

            assertThat(logs.events()).hasSize(2);
            assertThat(logs.events()).extracting(event -> logs.field(event, "keyword"))
                    .containsOnly("여백");
            assertThat(logs.events()).extracting(event -> logs.field(event, "searchUsed"))
                    .containsOnly(true);
            assertThat(logs.events()).extracting(event -> logs.field(event, "operation"))
                    .containsOnly("book.findBooks");
        }
    }
}
