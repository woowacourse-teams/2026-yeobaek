package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class SchemaMappingTest extends IntegrationTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "authors",
            "author_books",
            "books",
            "chapters",
            "passages",
            "sentences",
            "members",
            "member_blocks",
            "public_rooms",
            "public_room_members",
            "public_room_member_last_reads",
            "clubs",
            "club_books",
            "club_members",
            "club_member_last_reads",
            "comments",
            "public_room_comments",
            "club_comments",
            "comment_views",
            "reports"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("JPA 스키마는 승인된 20개 테이블로 구성된다")
    void mapsApprovedTables() {
        assertThat(jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = database() and table_type = 'BASE TABLE'
                """, String.class))
                .containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
    }

    @Test
    @DisplayName("댓글과 독서 상태는 승인된 소유 테이블과 컬럼으로 분리된다")
    void mapsNormalizedColumns() {
        Map<String, Set<String>> expected = Map.of(
                "comments", Set.of("id", "writer_id", "sentence_id", "content", "created_at", "updated_at"),
                "club_comments", Set.of("id", "comment_id", "club_id"),
                "public_room_comments", Set.of("id", "comment_id", "public_room_id"),
                "reports", Set.of("id", "reporter_id", "comment_id"),
                "clubs", Set.of("id", "name", "join_code"),
                "club_books", Set.of("id", "club_id", "book_id"),
                "club_members", Set.of("id", "member_id", "club_id", "status"),
                "club_member_last_reads",
                Set.of("id", "club_member_id", "last_read_passage_id", "last_read_at"),
                "public_room_members", Set.of("id", "member_id", "public_room_id", "last_visited_at"),
                "public_room_member_last_reads",
                Set.of("id", "public_room_member_id", "last_read_passage_id", "last_read_at"));
        Map<String, Set<String>> actual = expected.keySet().stream()
                .collect(Collectors.toMap(tableName -> tableName,
                        tableName -> Set.copyOf(columns(tableName))));

        assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @Test
    @DisplayName("일대일 소유 테이블의 부모 외래 키는 유일하다")
    void mapsUniqueParentForeignKeys() {
        assertThat(jdbcTemplate.queryForList("""
                select concat(table_name, ':', group_concat(column_name order by seq_in_index))
                from information_schema.statistics
                where table_schema = database() and non_unique = 0
                group by table_name, index_name
                """, String.class))
                .contains(
                        "public_rooms:book_id",
                        "club_books:club_id",
                        "club_member_last_reads:club_member_id",
                        "public_room_member_last_reads:public_room_member_id",
                        "club_comments:comment_id",
                        "public_room_comments:comment_id");
    }

    @Test
    @DisplayName("벌크 부모 삭제에 필요한 자식 외래 키는 DB cascade를 사용한다")
    void mapsDatabaseDeleteCascades() {
        assertThat(jdbcTemplate.queryForList("""
                select concat(kcu.table_name, ':', kcu.column_name)
                from information_schema.referential_constraints rc
                join information_schema.key_column_usage kcu
                  on kcu.constraint_schema = rc.constraint_schema
                 and kcu.table_name = rc.table_name
                 and kcu.constraint_name = rc.constraint_name
                where rc.constraint_schema = database() and rc.delete_rule = 'CASCADE'
                """, String.class))
                .contains(
                        "club_books:club_id",
                        "club_member_last_reads:club_member_id",
                        "public_room_member_last_reads:public_room_member_id",
                        "club_comments:comment_id",
                        "public_room_comments:comment_id",
                        "comment_views:comment_id",
                        "reports:comment_id");
    }

    private List<String> columns(String tableName) {
        return jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = database() and table_name = ?
                """, String.class, tableName);
    }
}
