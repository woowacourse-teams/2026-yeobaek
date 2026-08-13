package yeobaek.backend.support;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public ForbiddenException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }

    public ForbiddenException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
