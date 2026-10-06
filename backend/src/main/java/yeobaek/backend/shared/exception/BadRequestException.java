package yeobaek.backend.shared.exception;

import java.util.Map;

/**
 * 대상 부재 이외의 도메인 규칙 위반(중복 도서, 작가 중복 기재 등). HTTP 400 + 개별 코드로 응답한다 (API.md 0장).
 */
public class BadRequestException extends LogContextException {

    public BadRequestException(ErrorCode code, String message) {
        super(code, message);
    }

    public BadRequestException(ErrorCode code, String message, Map<String, String> logContext) {
        super(code, message, logContext);
    }

    public BadRequestException(ErrorCode code, String message, Throwable cause) {
        super(code, message, Map.of(), cause);
    }

    public BadRequestException(ErrorCode code, String message, Map<String, String> logContext, Throwable cause) {
        super(code, message, logContext, cause);
    }
}
