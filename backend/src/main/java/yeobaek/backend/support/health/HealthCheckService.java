package yeobaek.backend.support.health;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private static final String HEALTH_CHECK_QUERY = "SELECT 1";

    private final JdbcTemplate jdbcTemplate;

    public void checkDatabase() {
        log.atInfo().addKeyValue(OPERATION, "health.checkDatabase")
                .log("데이터베이스 헬스 체크를 시작합니다.");
        try {
            jdbcTemplate.queryForObject(HEALTH_CHECK_QUERY, Integer.class);
            log.atInfo().addKeyValue(OPERATION, "health.checkDatabase")
                    .addKeyValue(RESULT, SUCCESS)
                    .log("데이터베이스 헬스 체크를 완료했습니다.");
        } catch (DataAccessException exception) {
            log.atError().addKeyValue(OPERATION, "health.checkDatabase")
                    .addKeyValue(RESULT, "failure")
                    .addKeyValue("exceptionType", exception.getClass().getSimpleName())
                    .setCause(exception)
                    .log("데이터베이스 헬스 체크에 실패했습니다.");
            throw exception;
        }
    }
}
