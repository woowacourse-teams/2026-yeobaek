package yeobaek.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ReadingSchemaMigrationTest {

    private static final Path MIGRATION = Path.of(
            "docs", "migrations", "2026-10-12-reading-schema.sql");
    private static final Path BASELINE = Path.of(
            "src", "test", "resources", "migration", "2026-10-12-reading-schema-baseline.sql");
    private static final Path SEED = Path.of(
            "src", "test", "resources", "migration", "2026-10-12-reading-schema-seed.sql");
    private static final Path VERIFICATION = Path.of(
            "docs", "migrations", "2026-10-12-reading-schema-verify.sql");
    private static final Set<String> TARGET_TABLES = Set.of(
            "authors", "author_books", "books", "chapters", "passages", "sentences",
            "members", "member_blocks", "public_rooms", "public_room_members",
            "public_room_member_last_reads", "clubs", "club_books", "club_members",
            "club_member_last_reads", "comments", "public_room_comments", "club_comments",
            "comment_views", "reports");

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @BeforeEach
    void resetSchema() throws SQLException, IOException {
        try (Connection connection = connection()) {
            dropTables(connection);
            executeScript(connection, BASELINE);
            executeScript(connection, SEED);
        }
    }

    @Test
    void migratesHibernateAndDeployedVariantsAndRefusesRerun() throws Exception {
        for (boolean deployedCheck : List.of(false, true)) {
            if (deployedCheck) {
                resetSchema();
            }
            Map<String, Long> beforeRerun;
            try (Connection connection = connection()) {
                if (deployedCheck) {
                    addDeployedCommentCheck(connection);
                }
                executeScript(connection, MIGRATION);
                executeVerification(connection);
                verifyMigratedData(connection);
                verifyConstraintsAndAutoIncrement(connection);
                beforeRerun = databaseFingerprint(connection);
            }
            try (Connection rerunConnection = connection()) {
                assertThatThrownBy(() -> executeScript(rerunConnection, MIGRATION))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(databaseFingerprint(rerunConnection)).isEqualTo(beforeRerun);
                executeVerification(rerunConnection);
            }
        }
    }

    @Test
    void migratesSchemaWithoutComments() throws Exception {
        try (Connection connection = connection()) {
            dropTables(connection);
            executeScript(connection, BASELINE);

            executeScript(connection, MIGRATION);

            executeVerification(connection);
            assertThat(tableNames(connection)).isEqualTo(TARGET_TABLES);
            assertThat(queryLong(connection, "SELECT COUNT(*) FROM comments")).isZero();
        }
    }

    @Test
    void rejectsEmptyDatabaseBeforeAnyPermanentChange() throws Exception {
        try (Connection connection = connection()) {
            dropTables(connection);

            assertThatThrownBy(() -> executeScript(connection, MIGRATION))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(tableNames(connection)).isEmpty();
        }
    }

    @Test
    void rejectsInvalidSourceBeforeAnyPermanentChange() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE comments SET writer_id = 41 WHERE id = 90");

            assertThatThrownBy(() -> executeScript(connection, MIGRATION))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(tableNames(connection)).contains("comment_reports", "public_room_activities")
                    .doesNotContain("reports", "public_room_members", "club_books");
            assertThat(queryLong(connection, "SELECT writer_id FROM comments WHERE id = 90"))
                    .isEqualTo(41L);
        }
    }

    @Test
    void rejectsMalformedRenameIndexesBeforeAnyPermanentChange() throws Exception {
        for (String malformedIndex : List.of(
                "ALTER TABLE comment_reports "
                        + "RENAME INDEX uk_comment_reports_reporter_comment TO source_comment_reports_unique, "
                        + "ADD INDEX uk_comment_reports_reporter_comment (comment_id, reporter_id)",
                "ALTER TABLE public_room_activities "
                        + "RENAME INDEX uk_public_room_activities_member_room TO source_public_activities_unique, "
                        + "ADD INDEX uk_public_room_activities_member_room (public_room_id, member_id)")) {
            resetSchema();
            try (Connection connection = connection(); Statement statement = connection.createStatement()) {
                statement.execute(malformedIndex);
                Map<String, Long> beforeMigration = databaseFingerprint(connection);

                assertThatThrownBy(() -> executeScript(connection, MIGRATION))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(databaseFingerprint(connection)).isEqualTo(beforeMigration);
                assertThat(tableNames(connection)).contains("comment_reports", "public_room_activities")
                        .doesNotContain("reports", "public_room_members", "club_books");
            }
        }
    }

    private void verifyMigratedData(Connection connection) throws SQLException {
        assertThat(tableNames(connection)).isEqualTo(TARGET_TABLES);
        assertThat(queryStrings(connection, """
                SELECT CONCAT(id, ':', club_id, ':', book_id) FROM club_books ORDER BY id
                """)).containsExactly("60:60:10");
        assertThat(queryStrings(connection, """
                SELECT CONCAT(id, ':', club_member_id, ':', COALESCE(last_read_passage_id, 'null'),
                              ':', COALESCE(DATE_FORMAT(last_read_at, '%Y-%m-%d %H:%i:%s.%f'), 'null'))
                FROM club_member_last_reads ORDER BY id
                """)).containsExactly(
                        "70:70:30:2026-10-12 01:02:03.123456",
                        "71:71:31:null",
                        "72:72:null:2026-10-12 02:03:04.234567");
        assertThat(queryStrings(connection, """
                SELECT CONCAT(id, ':', public_room_member_id, ':',
                              COALESCE(last_read_passage_id, 'null'), ':',
                              COALESCE(DATE_FORMAT(last_read_at, '%Y-%m-%d %H:%i:%s.%f'), 'null'))
                FROM public_room_member_last_reads ORDER BY id
                """)).containsExactly(
                        "81:81:31:2026-10-12 04:05:06.456789",
                        "82:82:null:2026-10-12 05:06:07.567890",
                        "83:83:30:null");
        assertThat(queryStrings(connection, """
                SELECT CONCAT(c.id, ':', c.writer_id, ':', c.content, ':',
                              DATE_FORMAT(c.created_at, '%Y-%m-%d %H:%i:%s.%f'), ':',
                              COALESCE(DATE_FORMAT(c.updated_at, '%Y-%m-%d %H:%i:%s.%f'), 'null'))
                FROM comments c ORDER BY c.id
                """)).containsExactly(
                        "90:40:모임 댓글:2026-10-12 06:07:08.678901:2026-10-12 07:08:09.789012",
                        "91:41:공개 댓글:2026-10-12 08:09:10.890123:null",
                        "92:43:탈퇴 회원 댓글:2026-10-12 09:10:11.901234:null");
        assertThat(queryStrings(connection, """
                SELECT CONCAT(id, ':', member_id, ':', public_room_id, ':',
                              COALESCE(DATE_FORMAT(last_visited_at, '%Y-%m-%d %H:%i:%s.%f'), 'null'))
                FROM public_room_members ORDER BY id
                """)).containsExactly(
                        "80:42:50:2026-10-12 03:04:05.345678", "81:41:50:null",
                        "82:43:50:null", "83:40:50:null");
        assertThat(queryStrings(connection, """
                SELECT CONCAT(id, ':', member_id, ':', status) FROM club_members ORDER BY id
                """)).containsExactly("70:40:JOINED", "71:41:JOINED", "72:43:LEFT", "73:42:JOINED");
        assertThat(queryStrings(connection, """
                SELECT CONCAT(id, ':', blocker_id, ':', blocked_id) FROM member_blocks ORDER BY id
                """)).containsExactly("120:40:41");
        assertThat(queryStrings(connection, """
                SELECT CONCAT(id, ':', member_id, ':', comment_id) FROM comment_views ORDER BY id
                """)).containsExactly("110:42:91");
    }

    private void verifyConstraintsAndAutoIncrement(Connection connection) throws SQLException {
        assertThat(queryLong(connection, """
                SELECT COUNT(*) FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE() AND constraint_name = 'ck_comments_single_space'
                """)).isZero();
        assertThat(queryStrings(connection, """
                SELECT CONCAT(id, ':', reporter_id, ':', comment_id) FROM reports
                """)).containsExactly("100:40:91");
        assertThat(queryLong(connection, """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND CONCAT(table_name, '.', column_name) IN
                      ('clubs.book_id', 'club_members.last_read_passage_id',
                       'club_members.last_read_at', 'comments.club_member_id',
                       'comments.public_room_id', 'public_room_members.last_read_passage_id',
                       'public_room_members.last_read_at')
                """)).isZero();
        assertUniqueIndex(connection, "club_books", "uk_club_books_club", "club_id");
        assertUniqueIndex(connection, "club_member_last_reads", "uk_club_member_last_reads_member",
                "club_member_id");
        assertUniqueIndex(connection, "public_room_members", "uk_public_room_members_member_room",
                "member_id,public_room_id");
        assertUniqueIndex(connection, "public_room_member_last_reads",
                "uk_public_room_member_last_reads_member", "public_room_member_id");
        assertUniqueIndex(connection, "club_comments", "uk_club_comments_comment", "comment_id");
        assertUniqueIndex(connection, "public_room_comments", "uk_public_room_comments_comment", "comment_id");
        assertUniqueIndex(connection, "reports", "uk_reports_reporter_comment", "reporter_id,comment_id");

        assertForeignKey(connection, "club_books", "club_id", "clubs", "CASCADE");
        assertForeignKey(connection, "club_member_last_reads", "club_member_id", "club_members", "CASCADE");
        assertForeignKey(connection, "public_room_member_last_reads", "public_room_member_id",
                "public_room_members", "CASCADE");
        assertForeignKey(connection, "club_comments", "comment_id", "comments", "CASCADE");
        assertForeignKey(connection, "public_room_comments", "comment_id", "comments", "CASCADE");
        assertForeignKey(connection, "reports", "comment_id", "comments", "CASCADE");
        assertForeignKey(connection, "reports", "reporter_id", "members", "CASCADE");
        assertForeignKey(connection, "comment_views", "comment_id", "comments", "CASCADE");
        assertForeignKey(connection, "comment_views", "member_id", "members", "CASCADE");
        assertForeignKey(connection, "member_blocks", "blocker_id", "members", "CASCADE");
        assertForeignKey(connection, "member_blocks", "blocked_id", "members", "CASCADE");
        assertForeignKey(connection, "club_books", "book_id", "books", "NO ACTION");
        assertForeignKey(connection, "club_member_last_reads", "last_read_passage_id", "passages", "NO ACTION");
        assertForeignKey(connection, "public_room_member_last_reads", "last_read_passage_id",
                "passages", "NO ACTION");
        assertForeignKey(connection, "club_comments", "club_id", "clubs", "NO ACTION");
        assertForeignKey(connection, "public_room_comments", "public_room_id", "public_rooms", "NO ACTION");
        assertForeignKey(connection, "public_room_members", "member_id", "members", "NO ACTION");
        assertForeignKey(connection, "public_room_members", "public_room_id", "public_rooms", "NO ACTION");

        for (String table : List.of("club_books", "club_member_last_reads",
                "public_room_member_last_reads", "club_comments", "public_room_comments",
                "reports", "public_room_members")) {
            assertAutoIncrementAboveMaxId(connection, table);
        }
    }

    private void assertUniqueIndex(Connection connection, String table, String index, String columns)
            throws SQLException {
        assertThat(queryStrings(connection, """
                SELECT CONCAT(MAX(non_unique), ':', GROUP_CONCAT(column_name ORDER BY seq_in_index))
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ? AND index_name = ?
                GROUP BY table_name, index_name
                """, table, index)).containsExactly("0:" + columns);
    }

    private void assertForeignKey(Connection connection, String table, String column,
                                  String referencedTable, String deleteRule) throws SQLException {
        assertThat(queryStrings(connection, """
                SELECT CONCAT(kcu.referenced_table_name, ':', rc.delete_rule)
                FROM information_schema.key_column_usage kcu
                JOIN information_schema.referential_constraints rc
                  ON rc.constraint_schema = kcu.constraint_schema
                 AND rc.constraint_name = kcu.constraint_name
                 AND rc.table_name = kcu.table_name
                WHERE kcu.table_schema = DATABASE()
                  AND kcu.table_name = ? AND kcu.column_name = ?
                """, table, column)).containsExactly(referencedTable + ":" + deleteRule);
    }

    private void assertAutoIncrementAboveMaxId(Connection connection, String table) throws SQLException {
        long autoIncrement = queryLong(connection, """
                SELECT auto_increment FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, table);
        assertThat(autoIncrement).isGreaterThan(queryLong(connection,
                "SELECT COALESCE(MAX(id), 0) FROM `" + table + "`"));
    }

    private void executeVerification(Connection connection) throws IOException, SQLException {
        List<String> statements = readStatements(VERIFICATION);
        assertThat(statements).hasSize(3);
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(statements.getFirst())) {
            int checks = 0;
            while (result.next()) {
                assertThat(result.getLong("failure_count"))
                        .as(result.getString("check_name"))
                        .isZero();
                checks++;
            }
            assertThat(checks).isPositive();
        }
        assertThat(queryLong(connection, statements.get(1))).isEqualTo(20L);
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(statements.get(2))) {
            int columns = 0;
            while (result.next()) {
                columns++;
            }
            assertThat(columns).isPositive();
        }
    }

    private Map<String, Long> databaseFingerprint(Connection connection) throws SQLException {
        Map<String, Long> fingerprint = new TreeMap<>();
        for (String table : tableNames(connection)) {
            fingerprint.put(table, queryLong(connection,
                    "SELECT COUNT(*) FROM `" + table.replace("`", "``") + "`"));
        }
        return fingerprint;
    }

    private void addDeployedCommentCheck(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE comments ADD CONSTRAINT ck_comments_single_space CHECK (
                      (club_member_id IS NOT NULL AND public_room_id IS NULL AND writer_id IS NULL)
                      OR (club_member_id IS NULL AND public_room_id IS NOT NULL AND writer_id IS NOT NULL)
                    )
                    """);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private void dropTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String table : tableNames(connection)) {
                statement.execute("DROP TABLE `" + table.replace("`", "``") + "`");
            }
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    private void executeScript(Connection connection, Path path) throws IOException {
        for (String statementSql : readStatements(path)) {
            executeStatement(connection, path, statementSql);
        }
    }

    private List<String> readStatements(Path path) throws IOException {
        String sql = Files.readString(path).lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (left, right) -> left + right + '\n');
        List<String> statements = new ArrayList<>();
        for (String rawStatement : sql.split(";")) {
            String statementSql = rawStatement.trim();
            if (!statementSql.isEmpty()) {
                statements.add(statementSql);
            }
        }
        return statements;
    }

    private void executeStatement(Connection connection, Path path, String sql) {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException exception) {
            String snippet = sql.substring(0, Math.min(sql.length(), 120)).replace('\n', ' ');
            throw new IllegalStateException(path.getFileName() + " failed at: " + snippet, exception);
        }
    }

    private Set<String> tableNames(Connection connection) throws SQLException {
        Set<String> tables = new java.util.TreeSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")) {
            while (result.next()) {
                tables.add(result.getString(1));
            }
        }
        return tables;
    }

    private List<String> queryStrings(Connection connection, String sql, Object... parameters) throws SQLException {
        List<String> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    values.add(result.getString(1));
                }
            }
        }
        return values;
    }

    private long queryLong(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Expected one row but query returned none: " + sql);
                }
                return result.getLong(1);
            }
        }
    }

    private void bindParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
    }
}
