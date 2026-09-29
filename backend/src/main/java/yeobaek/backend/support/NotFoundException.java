package yeobaek.backend.support;

import java.util.Map;

/**
 * 요청이 가리키는 대상이 존재하지 않는 경우. HTTP 400 + 대상별 에러 코드로 응답한다 (API.md 0장).
 */
public class NotFoundException extends LogContextException {

    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }

    public NotFoundException(ErrorCode code, String message, Map<String, String> logContext) {
        super(code, message, logContext);
    }
}
