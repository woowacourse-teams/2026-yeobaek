package watson.backend.support;

import lombok.Getter;

/**
 * 인증 실패(관리자 토큰 누락·불일치). HTTP 401로 응답한다 (API.md 0장).
 */
@Getter
public class UnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public UnauthorizedException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }
}
