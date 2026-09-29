package yeobaek.backend.support;

import java.util.Map;
import lombok.Getter;

/**
 * 인증 실패(관리자 토큰 누락·불일치). HTTP 401로 응답한다 (API.md 0장).
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, String> logContext;

    public UnauthorizedException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public UnauthorizedException(ErrorCode code, String message, Map<String, String> logContext) {
        super(message);
        this.code = code;
        this.logContext = Map.copyOf(logContext);
    }
}
