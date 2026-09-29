package yeobaek.backend.admin.controller;

import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.PHASE;
import static yeobaek.backend.support.LogField.REASON;
import static yeobaek.backend.support.LogField.SUCCESS;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import yeobaek.backend.support.ErrorCode;
import yeobaek.backend.support.UnauthorizedException;

/**
 * 고정 관리자 토큰(X-Admin-Token) 검증. 토큰 누락·불일치는 401이며,
 * 서버에 토큰이 설정되지 않은 경우에도 전부 401이다 — 기동은 정상 (API.md 6장, 2026-08-06 결정).
 */
@RequiredArgsConstructor
@Slf4j
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final String adminToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.atInfo().addKeyValue(OPERATION, "auth.admin.preHandle")
                .log("관리자 인증을 시작합니다.");
        if (isConfigured() && adminToken.equals(request.getHeader(ADMIN_TOKEN_HEADER))) {
            log.atInfo().addKeyValue(OPERATION, "auth.admin.preHandle").addKeyValue(PHASE, SUCCESS)
                    .log("관리자 인증에 성공했습니다.");
            return true;
        }
        throw new UnauthorizedException(
                ErrorCode.UNAUTHORIZED,
                "관리자 토큰이 없거나 올바르지 않습니다.",
                Map.of(REASON, isConfigured() ? "invalid_admin_token" : "admin_token_not_configured"));
    }

    private boolean isConfigured() {
        return adminToken != null && !adminToken.isBlank();
    }
}
