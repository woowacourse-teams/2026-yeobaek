package yeobaek.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class BookDetailMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    private JdbcTemplate jdbc;

    @BeforeEach
    void prepareLegacySchema() throws SQLException {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        migration("5", 1_048_576).clean();
        try (Connection connection = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__init_schema.sql"));
        }
    }

    @Test
    @DisplayName("기존 책 데이터를 보존하면서 아직 집계하지 않은 본문 글자 수만 채운다")
    void preservesExistingBookDataWhenBackfillingCharacterCounts() {
        saveBook(1L, List.of(List.of("첫 문장.", "둘째 문장."), List.of("다음 문단.")));
        saveBook(2L, List.of(List.of("이미 집계한 본문")));
        Flyway details = migration("4", 1_048_576);
        details.baseline();
        details.migrate();
        jdbc.update("UPDATE books SET description = ? WHERE id = 1", "검수한 소개");
        jdbc.update("UPDATE books SET body_character_count = 42, description = ? WHERE id = 2", "기존 소개");
        List<Map<String, Object>> originalSentences = jdbc.queryForList("SELECT * FROM sentences ORDER BY id");
        List<Map<String, Object>> originalBooks = jdbc.queryForList(
                "SELECT id, title, passage_count, description FROM books ORDER BY id");

        assertThat(migration("5", 1_048_576).migrate().migrationsExecuted).isEqualTo(1);

        assertThat(jdbc.queryForList("SELECT body_character_count FROM books ORDER BY id", Long.class))
                .containsExactly(17L, 42L);
        assertThat(jdbc.queryForList("SELECT * FROM sentences ORDER BY id")).isEqualTo(originalSentences);
        assertThat(jdbc.queryForList("SELECT id, title, passage_count, description FROM books ORDER BY id"))
                .isEqualTo(originalBooks);
        assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history ORDER BY installed_rank", String.class))
                .containsExactly("1", "2", "3", "4", "5");
    }

    @Test
    @DisplayName("글자 수 보충에 실패하면 먼저 처리한 책도 원래대로 되돌리고 원인 해결 후 다시 실행할 수 있다")
    void rollsBackAllBookUpdatesAndCanRetryAfterFailure() {
        saveBook(1L, List.of(List.of("짧은 글")));
        saveBook(2L, List.of(List.of("한도를 넘기는 긴 문단")));
        Flyway details = migration("4", 1_048_576);
        details.baseline();
        details.migrate();
        Flyway limited = migration("5", 4);

        assertThatThrownBy(limited::migrate).isInstanceOf(FlywayException.class);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM books WHERE body_character_count IS NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 0", String.class)).containsExactly("5");

        Flyway retry = migration("5", 1_048_576);
        retry.repair();
        assertThat(retry.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT body_character_count FROM books ORDER BY id", Long.class))
                .containsExactly(4L, 12L);
    }

    private Flyway migration(String target, int paragraphBufferLimit) {
        return Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineVersion("1")
                .target(target)
                .cleanDisabled(false)
                .placeholders(Map.of("bookbodyparagraphbufferlimit", Integer.toString(paragraphBufferLimit)))
                .load();
    }

    private void saveBook(long bookId, List<List<String>> paragraphs) {
        jdbc.update("INSERT INTO books (id, title, passage_count) VALUES (?, ?, ?)",
                bookId, "기존 도서 " + bookId, paragraphs.size());
        jdbc.update("INSERT INTO chapters (id, book_id, sequence, title) VALUES (?, ?, 1, ?)",
                bookId, bookId, "본문");
        for (int index = 0; index < paragraphs.size(); index++) {
            long passageId = bookId * 100 + index;
            jdbc.update("INSERT INTO passages (id, chapter_id, sequence) VALUES (?, ?, ?)",
                    passageId, bookId, index + 1);
            List<String> sentences = paragraphs.get(index);
            for (int sentence = 0; sentence < sentences.size(); sentence++) {
                jdbc.update("INSERT INTO sentences (passage_id, sequence, content) VALUES (?, ?, ?)",
                        passageId, sentence + 1, sentences.get(sentence));
            }
        }
    }
}
