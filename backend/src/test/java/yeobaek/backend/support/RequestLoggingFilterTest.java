package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("추적 컨텍스트가 없으면 traceId를 만들지 않는다")
    void leaveTraceContextToLibrary() throws Exception {
        AtomicReference<String> captured = captureTraceId("/api/books");

        assertThat(captured.get()).isNull();
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("라이브러리의 traceId와 spanId를 변경하지 않는다")
    void preserveTraceContext() throws Exception {
        MDC.put("traceId", "library-trace");
        MDC.put("spanId", "library-span");

        assertThat(captureTraceId("/api/books").get()).isEqualTo("library-trace");
        assertThat(MDC.get("traceId")).isEqualTo("library-trace");
        assertThat(MDC.get("spanId")).isEqualTo("library-span");
    }

    @Test
    @DisplayName("인증 회원 MDC는 요청 종료 후 제거되어 다음 요청에 전달되지 않는다")
    void isolateMemberId() throws Exception {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/api/books");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, (request, response) ->
                MDC.put(RequestLoggingFilter.MEMBER_ID, "7"));
        AtomicReference<String> nextRequestMemberId = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/clubs"), new MockHttpServletResponse(),
                (request, response) -> nextRequestMemberId.set(MDC.get(RequestLoggingFilter.MEMBER_ID)));

        assertThat(MDC.get(RequestLoggingFilter.MEMBER_ID)).isNull();
        assertThat(nextRequestMemberId.get()).isNull();
    }

    @Test
    @DisplayName("처리되지 않은 예외는 ERROR와 500 상태로 기록하고 원래 예외를 전파한다")
    void logUnhandledFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (var logs = new LogCapture(RequestLoggingFilter.class.getName())) {
            assertThatThrownBy(() -> filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                throw new ServletException("failure-secret");
            })).isInstanceOf(ServletException.class);

            var failure = logs.event("http.doFilterInternal", "failure");
            assertThat(failure.getLevel()).isEqualTo(Level.ERROR);
            assertThat(logs.field(failure, "status")).isEqualTo(500);
            assertThat(logs.structuredText()).doesNotContain("private-secret");
        }
    }

    @Test
    @DisplayName("런타임 예외 후에도 회원 MDC를 정리하고 다음 요청에 전달하지 않는다")
    void clearMemberAfterRuntimeFailure() throws Exception {
        try (var logs = new LogCapture(RequestLoggingFilter.class.getName())) {
            assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(),
                    new MockHttpServletResponse(), (request, response) -> {
                        MDC.put(RequestLoggingFilter.MEMBER_ID, "7");
                        throw new IllegalStateException("failure");
                    })).isInstanceOf(IllegalStateException.class);
            assertThat(logs.field(logs.event("http.doFilterInternal", "failure"), "status")).isEqualTo(500);
        }
        assertThat(MDC.get(RequestLoggingFilter.MEMBER_ID)).isNull();
        assertThat(captureTraceId("/api/books").get()).isNull();
    }

    private AtomicReference<String> captureTraceId(String requestUri) throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            captured.set(MDC.get("traceId"));
            servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, requestUri);
        });
        return captured;
    }
}
