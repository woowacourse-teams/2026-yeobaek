package yeobaek.backend.support;

import lombok.Getter;

@Getter
public class TooManyRequestsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public TooManyRequestsException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }
}
