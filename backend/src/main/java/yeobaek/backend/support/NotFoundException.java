package yeobaek.backend.support;

import java.util.Map;
import lombok.Getter;

/**
 * 요청이 가리키는 대상이 존재하지 않는 경우. HTTP 400 + 대상별 에러 코드로 응답한다 (API.md 0장).
 */
@Getter
public class NotFoundException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, String> logContext;

    public NotFoundException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public NotFoundException(ErrorCode code, String message, Map<String, String> logContext) {
        super(message);
        this.code = code;
        this.logContext = Map.copyOf(logContext);
    }
}
