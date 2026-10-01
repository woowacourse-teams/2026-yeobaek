package yeobaek.backend.support.health;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class HealthCheckServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private HealthCheckService healthCheckService;

    @Test
    @DisplayName("데이터베이스 접근 예외를 변경하지 않고 전파한다")
    void propagateDatabaseAccessException() {
        var exception = new DataAccessResourceFailureException("database connection failed");
        given(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).willThrow(exception);

        assertThatThrownBy(healthCheckService::checkDatabase).isSameAs(exception);

        verify(jdbcTemplate).queryForObject("SELECT 1", Integer.class);
    }
}
