package yeobaek.backend.appreciation.api;

public final class UnsupportedAppreciationKindFailure extends RuntimeException {

    public UnsupportedAppreciationKindFailure(String kind) {
        this(kind, null);
    }

    public UnsupportedAppreciationKindFailure(String kind, Throwable cause) {
        super("지원하지 않는 감상 타입입니다: kind=" + kind, cause);
    }
}
