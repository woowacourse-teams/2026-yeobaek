package yeobaek.backend.readmodel.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcAdminClubStatisticsReadModel implements AdminClubStatisticsReadModel {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Map<Long, Long> countComments(List<Long> clubIds) {
        if (clubIds.isEmpty()) {
            return Map.of();
        }
        var parameters = new MapSqlParameterSource("clubIds", clubIds);
        return jdbcTemplate.query("""
                select club.id as club_id, count(comment.id) as comment_count
                from comments comment
                join appreciation_contexts context on context.appreciation_id = comment.id
                join clubs club on club.space_id = context.space_id
                where club.id in (:clubIds)
                group by club.id
                """, parameters, resultSet -> {
                    Map<Long, Long> counts = new HashMap<>();
                    while (resultSet.next()) {
                        counts.put(resultSet.getLong("club_id"), resultSet.getLong("comment_count"));
                    }
                    return Map.copyOf(counts);
                });
    }

    @Override
    public Map<Long, Long> countClubsByBookIds(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        var parameters = new MapSqlParameterSource("bookIds", bookIds);
        return jdbcTemplate.query("""
                select book.id as book_id, count(binding.space_id) as club_count
                from books book
                join club_content_bindings binding on binding.content_id = book.content_id
                where book.id in (:bookIds)
                group by book.id
                """, parameters, resultSet -> {
                    Map<Long, Long> counts = new HashMap<>();
                    while (resultSet.next()) {
                        counts.put(resultSet.getLong("book_id"), resultSet.getLong("club_count"));
                    }
                    return Map.copyOf(counts);
                });
    }
}
