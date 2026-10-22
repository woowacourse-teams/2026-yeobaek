package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * 기존 도서도 환산 쪽수를 제공할 수 있도록 Flyway가 미집계 본문 글자 수를 한 번 보충한다.
 */
public class V5__BackfillBookBodyCharacterCount extends BaseJavaMigration {

    private static final int DEFAULT_PARAGRAPH_BUFFER_LIMIT = 1_048_576;

    @Override
    public void migrate(Context context) throws SQLException {
        int bufferLimit = paragraphBufferLimit(context);
        long cursor = 0;
        Long bookId = nextBook(context.getConnection(), cursor);
        while (bookId != null) {
            long count = countBook(context.getConnection(), bookId, bufferLimit);
            save(context.getConnection(), bookId, count);
            cursor = bookId;
            bookId = nextBook(context.getConnection(), cursor);
        }
    }

    private int paragraphBufferLimit(Context context) {
        String configured = context.getConfiguration().getPlaceholders().get("bookbodyparagraphbufferlimit");
        if (configured == null) {
            return DEFAULT_PARAGRAPH_BUFFER_LIMIT;
        }
        int limit;
        try {
            limit = Integer.parseInt(configured);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("bookbodyparagraphbufferlimit must be a positive integer", invalid);
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("bookbodyparagraphbufferlimit must be a positive integer");
        }
        return limit;
    }

    private Long nextBook(Connection connection, long after) throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT id FROM books WHERE body_character_count IS NULL AND id > ? ORDER BY id LIMIT 1
                """)) {
            query.setLong(1, after);
            try (var rows = query.executeQuery()) {
                return rows.next() ? rows.getLong(1) : null;
            }
        }
    }

    private long countBook(Connection connection, long bookId, int bufferLimit) throws SQLException {
        long passageId = 0;
        try (var query = connection.prepareStatement("""
                SELECT p.id AS passage_id, s.content
                FROM chapters c LEFT JOIN passages p ON p.chapter_id = c.id
                LEFT JOIN sentences s ON s.passage_id = p.id
                WHERE c.book_id = ? ORDER BY p.sequence, p.id, s.sequence, s.id
                """, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            query.setLong(1, bookId);
            // Connector/J streams rows; close the result before issuing another statement.
            query.setFetchSize(Integer.MIN_VALUE);
            try (var rows = query.executeQuery()) {
                long total = 0;
                var paragraph = new StringBuilder();
                while (rows.next()) {
                    long nextPassage = rows.getLong("passage_id");
                    if (rows.wasNull()) {
                        throw invalidBody(bookId, 0, "Chapter has no passages");
                    }
                    if (passageId != 0 && passageId != nextPassage) {
                        total = Math.addExact(total, countParagraph(paragraph));
                        paragraph.setLength(0);
                    }
                    passageId = nextPassage;
                    String content = rows.getString("content");
                    if (content == null || content.isBlank()) {
                        throw invalidBody(bookId, passageId, "Passage has a missing or blank sentence");
                    }
                    if (content.length() > bufferLimit - paragraph.length()) {
                        throw invalidBody(bookId, passageId,
                                "Paragraph exceeds bookbodyparagraphbufferlimit=" + bufferLimit + " UTF-16 units");
                    }
                    paragraph.append(content);
                }
                total = Math.addExact(total, countParagraph(paragraph));
                if (total <= 0) {
                    throw invalidBody(bookId, passageId, "Book has no body");
                }
                return total;
            }
        } catch (ArithmeticException overflow) {
            throw new IllegalStateException("Book body backfill failed bookId=" + bookId
                    + " passageId=" + passageId + ": Body character count overflow", overflow);
        } catch (SQLException failure) {
            throw new SQLException("Book body backfill failed bookId=" + bookId + " passageId=" + passageId
                    + " SQLState=" + failure.getSQLState(), failure.getSQLState(), failure.getErrorCode(), failure);
        }
    }

    private long countParagraph(StringBuilder paragraph) {
        // Freeze this migration's algorithm independently of later application changes.
        return Normalizer.normalize(paragraph, Normalizer.Form.NFC).codePoints()
                .filter(point -> point != '\r' && point != '\n' && point != '\t').count();
    }

    private void save(Connection connection, long bookId, long count) throws SQLException {
        try (var update = connection.prepareStatement("""
                UPDATE books SET body_character_count = ? WHERE id = ? AND body_character_count IS NULL
                """)) {
            update.setLong(1, count);
            update.setLong(2, bookId);
            update.executeUpdate();
        }
    }

    private IllegalStateException invalidBody(long bookId, long passageId, String reason) {
        return new IllegalStateException("Book body backfill failed bookId=" + bookId
                + " passageId=" + passageId + ": " + reason);
    }
}
