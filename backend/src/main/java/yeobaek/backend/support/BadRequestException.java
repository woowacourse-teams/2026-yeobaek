package yeobaek.backend.support;

import lombok.Getter;

/**
 * 대상 부재 이외의 도메인 규칙 위반(중복 도서, 작가 중복 기재 등). HTTP 400 + 개별 코드로 응답한다 (API.md 0장).
 */
@Getter
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public BadRequestException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }
}
