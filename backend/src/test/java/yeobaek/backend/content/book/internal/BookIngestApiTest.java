package yeobaek.backend.content.book.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import yeobaek.backend.content.api.book.BookIngestApi;
import yeobaek.backend.content.api.book.BookIngestApi.AuthorReference;
import yeobaek.backend.content.api.book.BookIngestApi.ChapterInput;
import yeobaek.backend.content.api.book.BookIngestApi.Command;
import yeobaek.backend.content.api.book.BookIngestApi.PassageInput;
import yeobaek.backend.content.api.book.BookIngestApi.SentenceInput;
import yeobaek.backend.content.api.book.BookIngestException;
import yeobaek.backend.content.api.value.AuthorName;
import yeobaek.backend.content.api.value.BookTitle;
import yeobaek.backend.content.api.value.ChapterTitle;
import yeobaek.backend.content.api.value.SentenceContent;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.support.IntegrationTest;

class BookIngestApiTest extends IntegrationTest {

    @Autowired
    private BookIngestApi ingestApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsBookAndContentLocationsThroughTheReusableModuleApi() {
        var result = ingestApi.ingest(command(List.of(new AuthorReference(null, new AuthorName("작가"), null))));

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
                .isInstanceOf(BookIngestException.class)
                .satisfies(failure -> {
                    var ingestFailure = (BookIngestException) failure;
                    assertThat(ingestFailure.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                    assertThat(ingestFailure.getLogContext()).containsEntry("reason", "AUTHORS_EMPTY");
                });
        assertThat(List.of(count("contents"), count("books"), count("content_locations"))).containsOnly(0L);
    }

    @Test
    void rollsBackTheAllocatedContentRootWhenAnAuthorReferenceFails() {
        assertThatThrownBy(() -> ingestApi.ingest(command(List.of(new AuthorReference(9999L, null, null)))))
                .isInstanceOf(BookIngestException.class)
                .extracting(failure -> ((BookIngestException) failure).getCode())
                .isEqualTo(ErrorCode.AUTHOR_NOT_FOUND);
        assertThat(List.of(count("contents"), count("books"), count("authors"), count("content_locations")))
                .containsOnly(0L);
    }

    private Command command(List<AuthorReference> authors) {
        var sentences = List.of(new SentenceInput(new SentenceContent("첫 문장.")),
                new SentenceInput(new SentenceContent("둘째 문장.")));
        var chapters = List.of(new ChapterInput(new ChapterTitle("1장"), List.of(new PassageInput(sentences))));
        return new Command(new BookTitle("모듈 도서"), null, 2026, null, authors, chapters);
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }
}
