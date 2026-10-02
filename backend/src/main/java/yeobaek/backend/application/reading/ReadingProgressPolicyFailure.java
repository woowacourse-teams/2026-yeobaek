package yeobaek.backend.application.reading;

public final class ReadingProgressPolicyFailure extends RuntimeException {

    public enum Reason {
        SPACE_ACCESS_DENIED,
        CONTENT_NOT_BOUND,
        LOCATION_NOT_IN_CONTENT
    }

    private final transient Reason failureReason;

    public ReadingProgressPolicyFailure(Reason reason, String message) {
        super(message);
        this.failureReason = reason;
    }

    public Reason reason() {
        return failureReason;
    }
}
