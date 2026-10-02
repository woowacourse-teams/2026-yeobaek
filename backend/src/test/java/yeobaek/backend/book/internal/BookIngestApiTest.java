package yeobaek.backend.book.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.book.api.BookIngestApi;
import yeobaek.backend.book.api.BookIngestApi.AuthorReference;
import yeobaek.backend.book.api.BookIngestApi.ChapterInput;
import yeobaek.backend.book.api.BookIngestApi.Command;
import yeobaek.backend.book.api.BookIngestApi.PassageInput;
import yeobaek.backend.book.api.BookIngestApi.SentenceInput;
import yeobaek.backend.book.api.BookIngestFailure;
import yeobaek.backend.support.IntegrationTest;

class BookIngestApiTest extends IntegrationTest {

    @Autowired
    private BookIngestApi ingestApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsBookAndContentLocationsThroughTheReusableModuleApi() {
        var result = ingestApi.ingest(command(List.of(new AuthorReference(null, "작가", null))));

        assertThat(result.title()).isEqualTo("모듈 도서");
        assertThat(result.passageCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select content_id from books where id = ?", Long.class,
                result.bookId())).isEqualTo(result.contentId().value());
        assertThat(jdbcTemplate.queryForObject("select count(*) from content_locations where content_id = ?",
                Long.class, result.contentId().value())).isEqualTo(3L);
        assertThat(List.of(count("public_rooms"), count("space_content_bindings"))).containsOnly(0L);
    }

    @Test
    void rejectsEmptyAuthorsWithoutRelyingOnAdminRequestValidation() {
        assertThatThrownBy(() -> ingestApi.ingest(command(List.of())))
                .isInstanceOf(BookIngestFailure.class)
                .extracting(failure -> ((BookIngestFailure) failure).reason())
                .isEqualTo(BookIngestFailure.Reason.AUTHORS_EMPTY);
        assertThat(List.of(count("contents"), count("books"), count("content_locations"))).containsOnly(0L);
    }

    @Test
    void rollsBackTheAllocatedContentRootWhenAnAuthorReferenceFails() {
        assertThatThrownBy(() -> ingestApi.ingest(command(List.of(new AuthorReference(9999L, null, null)))))
                .isInstanceOf(BookIngestFailure.class)
                .extracting(failure -> ((BookIngestFailure) failure).reason())
                .isEqualTo(BookIngestFailure.Reason.AUTHOR_NOT_FOUND);
        assertThat(List.of(count("contents"), count("books"), count("authors"), count("content_locations")))
                .containsOnly(0L);
    }

    private Command command(List<AuthorReference> authors) {
        var sentences = List.of(new SentenceInput("첫 문장."), new SentenceInput("둘째 문장."));
        var chapters = List.of(new ChapterInput("1장", List.of(new PassageInput(sentences))));
        return new Command("모듈 도서", null, 2026, null, authors, chapters);
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }
}
