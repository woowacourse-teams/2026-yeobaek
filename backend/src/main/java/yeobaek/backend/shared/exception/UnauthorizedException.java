package yeobaek.backend.shared.exception;

import java.util.Map;

/**
 * 인증 실패(관리자 토큰 누락·불일치). HTTP 401로 응답한다 (API.md 0장).
 */
public class UnauthorizedException extends LogContextException {

    public UnauthorizedException(ErrorCode code, String message) {
        super(code, message);
    }

    public UnauthorizedException(ErrorCode code, String message, Map<String, String> logContext) {
        super(code, message, logContext);
    }
}
