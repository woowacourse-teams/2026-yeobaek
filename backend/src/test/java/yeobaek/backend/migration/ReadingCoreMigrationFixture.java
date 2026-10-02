package yeobaek.backend.migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.testcontainers.containers.JdbcDatabaseContainer;

/** Standalone legacy schema and SQL runner shared by migration and new-application startup tests. */
public final class ReadingCoreMigrationFixture implements AutoCloseable {

    public static final Path MIGRATION = Path.of("docs/migrations/2026-10-02-core-modules.sql");
    public static final Path VERIFICATION = Path.of("docs/migrations/2026-10-02-core-modules-verify.sql");

    private final Connection connection;

    public ReadingCoreMigrationFixture(JdbcDatabaseContainer<?> database) throws SQLException {
        connection = DriverManager.getConnection(database.getJdbcUrl(), database.getUsername(), database.getPassword());
    }

    public void resetLegacySchema() throws SQLException {
        execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : tableNames()) {
            execute("DROP TABLE IF EXISTS " + table);
        }
        execute("SET FOREIGN_KEY_CHECKS = 1");
        executeLegacySchema();
    }

    public void executeLegacySchema() throws SQLException {
        execute("CREATE TABLE members (id BIGINT AUTO_INCREMENT PRIMARY KEY, nickname VARCHAR(50) NOT NULL)");
        execute("CREATE TABLE books (id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(255) NOT NULL, "
                + "publisher VARCHAR(255), published_year INT, cover_image_key VARCHAR(80), "
                + "passage_count INT NOT NULL, status VARCHAR(20) NOT NULL)");
        execute("CREATE TABLE chapters (id BIGINT AUTO_INCREMENT PRIMARY KEY, book_id BIGINT NOT NULL, "
                + "title VARCHAR(255) NOT NULL, sequence INT NOT NULL, "
                + "FOREIGN KEY (book_id) REFERENCES books(id))");
        execute("CREATE TABLE passages (id BIGINT AUTO_INCREMENT PRIMARY KEY, chapter_id BIGINT NOT NULL, "
                + "sequence INT NOT NULL, FOREIGN KEY (chapter_id) REFERENCES chapters(id))");
        execute("CREATE TABLE sentences (id BIGINT AUTO_INCREMENT PRIMARY KEY, passage_id BIGINT NOT NULL, "
                + "sequence INT NOT NULL, content TEXT NOT NULL, "
                + "FOREIGN KEY (passage_id) REFERENCES passages(id), UNIQUE (passage_id, sequence))");
        execute("CREATE TABLE clubs (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL, "
                + "book_id BIGINT NOT NULL, join_code VARCHAR(10) NOT NULL, "
                + "FOREIGN KEY (book_id) REFERENCES books(id), UNIQUE (join_code))");
        execute("CREATE TABLE club_members (id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT NOT NULL, "
                + "club_id BIGINT NOT NULL, last_read_passage_id BIGINT, last_read_at DATETIME(6), "
                + "status VARCHAR(20) NOT NULL DEFAULT 'JOINED', "
                + "FOREIGN KEY (member_id) REFERENCES members(id), FOREIGN KEY (club_id) REFERENCES clubs(id), "
                + "FOREIGN KEY (last_read_passage_id) REFERENCES passages(id), UNIQUE (member_id, club_id))");
        execute("CREATE TABLE public_rooms (id BIGINT AUTO_INCREMENT PRIMARY KEY, book_id BIGINT NOT NULL, "
                + "FOREIGN KEY (book_id) REFERENCES books(id), UNIQUE (book_id))");
        execute("CREATE TABLE public_room_activities (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "member_id BIGINT NOT NULL, public_room_id BIGINT NOT NULL, last_visited_at DATETIME(6), "
                + "last_read_passage_id BIGINT, last_read_at DATETIME(6), "
                + "FOREIGN KEY (member_id) REFERENCES members(id), "
                + "FOREIGN KEY (public_room_id) REFERENCES public_rooms(id), "
                + "FOREIGN KEY (last_read_passage_id) REFERENCES passages(id), "
                + "UNIQUE (member_id, public_room_id))");
        execute("CREATE TABLE comments (id BIGINT AUTO_INCREMENT PRIMARY KEY, club_member_id BIGINT, "
                + "public_room_id BIGINT, writer_id BIGINT, sentence_id BIGINT NOT NULL, "
                + "content VARCHAR(1000) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6), "
                + "FOREIGN KEY (club_member_id) REFERENCES club_members(id), "
                + "FOREIGN KEY (public_room_id) REFERENCES public_rooms(id), "
                + "FOREIGN KEY (writer_id) REFERENCES members(id), "
                + "FOREIGN KEY (sentence_id) REFERENCES sentences(id), "
                + "CONSTRAINT ck_comments_single_space CHECK ((club_member_id IS NOT NULL "
                + "AND public_room_id IS NULL AND writer_id IS NULL) OR (club_member_id IS NULL "
                + "AND public_room_id IS NOT NULL AND writer_id IS NOT NULL)))");
        execute("CREATE TABLE comment_views (id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT NOT NULL, "
                + "comment_id BIGINT NOT NULL, FOREIGN KEY (member_id) REFERENCES members(id), "
                + "FOREIGN KEY (comment_id) REFERENCES comments(id), INDEX (member_id, comment_id))");
        execute("CREATE TABLE comment_reports (id BIGINT AUTO_INCREMENT PRIMARY KEY, reporter_id BIGINT NOT NULL, "
                + "comment_id BIGINT NOT NULL, FOREIGN KEY (reporter_id) REFERENCES members(id), "
                + "FOREIGN KEY (comment_id) REFERENCES comments(id), UNIQUE (reporter_id, comment_id))");
    }

    public void seedRepresentativeLegacyData() throws SQLException {
        execute("INSERT INTO members (id, nickname) VALUES (1, 'one'), (2, 'two'), (3, 'three')");
        execute("INSERT INTO books (id, title, passage_count, status) VALUES "
                + "(1, 'active', 1, 'ACTIVE'), (2, 'deleted', 1, 'DELETED')");
        execute("INSERT INTO chapters (id, book_id, title, sequence) VALUES "
                + "(1, 1, 'active chapter', 1), (2, 2, 'deleted chapter', 1)");
        execute("INSERT INTO passages (id, chapter_id, sequence) VALUES (1, 1, 1), (2, 2, 1)");
        execute("INSERT INTO sentences (id, passage_id, sequence, content) VALUES "
                + "(1, 1, 1, 'active sentence'), (2, 2, 1, 'deleted sentence')");
        execute("INSERT INTO clubs (id, name, book_id, join_code) VALUES (1, 'club', 2, 'ABC123')");
        execute("INSERT INTO public_rooms (id, book_id) VALUES (1, 1)");
        execute("INSERT INTO club_members (id, member_id, club_id, last_read_passage_id, last_read_at, status) "
                + "VALUES (10, 1, 1, 2, '2026-09-01 10:00:00.000001', 'LEFT'), "
                + "(11, 3, 1, NULL, NULL, 'JOINED')");
        execute("INSERT INTO public_room_activities "
                + "(id, member_id, public_room_id, last_visited_at, last_read_passage_id, last_read_at) VALUES "
                + "(20, 2, 1, NULL, NULL, NULL), "
                + "(21, 3, 1, '2026-09-02 11:00:00.000002', 1, '2026-09-02 10:00:00.000003')");
        execute("INSERT INTO comments (id, club_member_id, public_room_id, writer_id, sentence_id, "
                + "content, created_at, updated_at) VALUES "
                + "(100, 10, NULL, NULL, 2, 'club comment', '2026-09-03 12:00:00.000004', NULL), "
                + "(101, NULL, 1, 2, 1, 'room comment', '2026-09-04 13:00:00.000005', "
                + "'2026-09-04 14:00:00.000006')");
        execute("INSERT INTO comment_views (id, member_id, comment_id) VALUES "
                + "(1000, 3, 100), (1001, 3, 100), (1002, 1, 101), (1003, 3, 101)");
        execute("INSERT INTO comment_reports (id, reporter_id, comment_id) VALUES "
                + "(2000, 3, 101), (2001, 1, 101)");
    }

    public void executeScript(Path path) throws IOException, SQLException {
        String sql = Files.readString(path, StandardCharsets.UTF_8)
                .replaceAll("(?m)^\\s*--.*$", "");
        for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
                execute(statement);
            }
        }
    }

    public void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public long queryLong(String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new AssertionError("Expected one result row for: " + sql);
            }
            return resultSet.getLong(1);
        }
    }

    public boolean tableExists(String tableName) throws SQLException {
        return metadataCount(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = ?",
                tableName) == 1;
    }

    public boolean columnExists(String tableName, String columnName) throws SQLException {
        return metadataCount(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                tableName, columnName) == 1;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    private long metadataCount(String sql, String... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setString(index + 1, parameters[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new AssertionError("Expected one metadata count row");
                }
                return resultSet.getLong(1);
            }
        }
    }

    private List<String> tableNames() {
        return List.of(
                "appreciation_reports", "appreciation_views", "public_room_visits", "reading_progresses",
                "appreciation_contexts", "public_room_content_bindings", "club_content_bindings",
                "space_content_bindings", "comments", "comment_reports", "comment_views", "appreciations",
                "public_room_activities", "club_members", "public_rooms", "clubs", "sentences", "passages",
                "content_locations", "chapters", "books", "contents", "member_blocks", "members", "spaces",
                "core_migration_backup_20261002_clubs", "core_migration_backup_20261002_public_rooms",
                "core_migration_backup_20261002_books", "core_migration_backup_20261002_passages",
                "core_migration_backup_20261002_sentences", "core_migration_backup_20261002_comments",
                "core_migration_backup_20261002_club_members",
                "core_migration_backup_20261002_public_room_activities",
                "core_migration_backup_20261002_comment_views",
                "core_migration_backup_20261002_comment_reports");
    }
}
