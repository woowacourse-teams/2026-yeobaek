package yeobaek.backend.support;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.SUCCESS;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String MEMBER_ID = LogField.MEMBER_ID;
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String previousTraceId = MDC.get(TRACE_ID);
        String previousMemberId = MDC.get(MEMBER_ID);
        String traceId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put(TRACE_ID, traceId);
        MDC.remove(MEMBER_ID);
        LOGGER.atInfo()
                .addKeyValue(OPERATION, "http.doFilterInternal")
                .addKeyValue("httpMethod", request.getMethod())
                .log("HTTP 요청을 시작합니다.");
        boolean completed = false;
        Exception failure = null;
        try {
            filterChain.doFilter(request, response);
            completed = true;
        } catch (ServletException | IOException exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                int status = completed ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                logCompletion(request, status, startedAt, failure);
            } finally {
                restoreMdc(TRACE_ID, previousTraceId);
                restoreMdc(MEMBER_ID, previousMemberId);
            }
        }
    }

    private void logCompletion(HttpServletRequest request, int status, long startedAt, Exception failure) {
        var event = status >= 500 ? LOGGER.atError() : LOGGER.atInfo();
        event.addKeyValue(OPERATION, "http.doFilterInternal")
                .addKeyValue(RESULT, status >= 500 ? "failure" : SUCCESS)
                .addKeyValue("httpMethod", request.getMethod())
                .addKeyValue("route", route(request))
                .addKeyValue("status", status)
                .addKeyValue("durationMs", elapsedMillis(startedAt))
                .setCause(failure)
                .log("HTTP 요청 처리를 완료했습니다.");
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern == null ? "unmatched" : pattern.toString();
    }

    private void restoreMdc(String key, String previousValue) {
        if (previousValue == null) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, previousValue);
    }
}
