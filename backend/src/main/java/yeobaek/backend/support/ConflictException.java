package yeobaek.backend.support;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public ConflictException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }

    public ConflictException(ErrorCode code, Throwable cause) {
        super(code.getDefaultMessage(), cause);
        this.code = code;
    }
}
