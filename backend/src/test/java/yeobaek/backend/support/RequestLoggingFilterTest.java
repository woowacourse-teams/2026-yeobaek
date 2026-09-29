package yeobaek.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.read.ListAppender;
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
    private LogCapture attachedAppender;

    @AfterEach
    void clearMdc() {
        MDC.clear();
        if (attachedAppender != null) {
            attachedAppender.close();
        }
    }

    @Test
    @DisplayName("요청마다 다른 traceId를 부여하고 완료 후 기존 MDC를 복원한다")
    void isolateTraceId() throws Exception {
        MDC.put(RequestLoggingFilter.TRACE_ID, "outer-trace");
        AtomicReference<String> first = captureTraceId("/api/books");
        AtomicReference<String> second = captureTraceId("/api/clubs");

        assertThat(first.get()).isNotBlank().isNotEqualTo("outer-trace");
        assertThat(second.get()).isNotBlank().isNotEqualTo(first.get());
        assertThat(MDC.get(RequestLoggingFilter.TRACE_ID)).isEqualTo("outer-trace");
        assertThat(MDC.get(RequestLoggingFilter.MEMBER_ID)).isNull();
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
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = attachAppender();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw new ServletException("failure-secret");
        })).isInstanceOf(ServletException.class);

        var failure = appender.list.getLast();
        assertThat(failure.getLevel()).isEqualTo(Level.ERROR);
        assertThat(keyValue(failure, "status")).isEqualTo(500);
        assertThat(failure.getFormattedMessage()).doesNotContain("private-secret", "failure-secret");
    }

    private AtomicReference<String> captureTraceId(String requestUri) throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            captured.set(MDC.get(RequestLoggingFilter.TRACE_ID));
            servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, requestUri);
        });
        return captured;
    }

    private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> attachAppender() {
        attachedAppender = new LogCapture(RequestLoggingFilter.class.getName());
        return attachedAppender;
    }

    private Object keyValue(ch.qos.logback.classic.spi.ILoggingEvent event, String key) {
        return event.getKeyValuePairs().stream()
                .filter(pair -> pair.key.equals(key))
                .map(pair -> pair.value)
                .findFirst()
                .orElse(null);
    }
}
