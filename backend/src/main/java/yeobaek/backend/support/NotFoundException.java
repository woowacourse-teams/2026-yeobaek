package yeobaek.backend.support;

import lombok.Getter;

/**
 * 요청이 가리키는 대상이 존재하지 않는 경우. HTTP 400 + 대상별 에러 코드로 응답한다 (API.md 0장).
 */
@Getter
public class NotFoundException extends RuntimeException {

    private final ErrorCode code;

    public NotFoundException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
