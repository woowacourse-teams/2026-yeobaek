package yeobaek.backend.support.health;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import yeobaek.backend.support.ControllerTest;

@WebMvcTest(HealthController.class)
class HealthControllerTest extends ControllerTest {

    @MockitoBean
    private HealthCheckService healthCheckService;

    @Test
    @DisplayName("인증 헤더 없이 서버와 데이터베이스가 정상이면 빈 200 응답을 반환한다")
    void healthCheckSucceedsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(healthCheckService, times(1)).checkDatabase();
    }

    @Test
    @DisplayName("데이터베이스 접근에 실패하면 내부 오류 정보를 노출하지 않고 빈 503 응답을 반환한다")
    void healthCheckReturnsServiceUnavailableWhenDatabaseAccessFails() throws Exception {
        willThrow(new DataAccessResourceFailureException(
                "jdbc:mysql://database.internal:3306/yeobaek connection refused"))
                .given(healthCheckService).checkDatabase();

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));

        verify(healthCheckService, times(1)).checkDatabase();
    }

    @Test
    @DisplayName("데이터베이스 접근 예외가 아닌 서비스 예외를 변경하지 않고 전파한다")
    void propagateUnexpectedServiceException() throws Exception {
        var serviceException = new IllegalStateException("unexpected health check failure");
        willThrow(serviceException).given(healthCheckService).checkDatabase();

        var result = mockMvc.perform(get("/health")).andReturn();

        assertSame(serviceException, result.getResolvedException(),
                "컨트롤러는 데이터베이스 접근 예외가 아닌 서비스 예외를 변경하지 않아야 한다");
        verify(healthCheckService, times(1)).checkDatabase();
    }
}
