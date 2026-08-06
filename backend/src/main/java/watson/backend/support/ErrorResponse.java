package watson.backend.support;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.name(), message);
    }
}
