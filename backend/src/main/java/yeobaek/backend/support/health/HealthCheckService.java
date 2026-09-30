package yeobaek.backend.support.health;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private static final String HEALTH_CHECK_QUERY = "SELECT 1";

    private final JdbcTemplate jdbcTemplate;

    public void checkDatabase() {
        jdbcTemplate.queryForObject(HEALTH_CHECK_QUERY, Integer.class);
    }
}
