package yeobaek.backend.support;

import lombok.Getter;

@Getter
public class TooManyRequestsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public TooManyRequestsException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
