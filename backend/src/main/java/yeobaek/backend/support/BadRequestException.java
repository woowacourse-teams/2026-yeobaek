package yeobaek.backend.support;

import java.util.Map;
import lombok.Getter;

/**
 * 대상 부재 이외의 도메인 규칙 위반(중복 도서, 작가 중복 기재 등). HTTP 400 + 개별 코드로 응답한다 (API.md 0장).
 */
@Getter
public class BadRequestException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, String> logContext;

    public BadRequestException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public BadRequestException(ErrorCode code, String message, Map<String, String> logContext) {
        super(message);
        this.code = code;
        this.logContext = Map.copyOf(logContext);
    }
}
