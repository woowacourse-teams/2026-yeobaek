package yeobaek.backend.support;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public ConflictException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ConflictException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
