package yeobaek.backend.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@WebMvcTest(GlobalExceptionHandler.class)
class MissingRouteTest extends ControllerTest {

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    @DisplayName("없는 경로에 요청하면 404와 ROUTE_NOT_FOUND를 반환한다")
    void missingRouteReturnsNotFound(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), "/wiki"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("요청한 경로를 찾을 수 없습니다."));
    }
}
