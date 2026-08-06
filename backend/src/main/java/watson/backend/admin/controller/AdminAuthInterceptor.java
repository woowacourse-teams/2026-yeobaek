package watson.backend.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;
import watson.backend.support.ErrorCode;
import watson.backend.support.UnauthorizedException;

/**
 * 고정 관리자 토큰(X-Admin-Token) 검증. 토큰 누락·불일치는 401이며,
 * 서버에 토큰이 설정되지 않은 경우에도 전부 401이다 — 기동은 정상 (API.md 6장, 2026-08-06 결정).
 */
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final String adminToken;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isConfigured() && adminToken.equals(request.getHeader(ADMIN_TOKEN_HEADER))) {
            return true;
        }
        throw new UnauthorizedException(ErrorCode.UNAUTHORIZED);
    }

    private boolean isConfigured() {
        return adminToken != null && !adminToken.isBlank();
    }
}
