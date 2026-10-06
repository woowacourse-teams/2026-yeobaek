package yeobaek.backend.application.space.query;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.space.api.SpaceKind;

public final class UnsupportedSpaceQueryException extends LogContextException {

    private final transient SpaceKind unsupportedKind;

    public UnsupportedSpaceQueryException(SpaceKind kind, String message) {
        super(ErrorCode.UNSUPPORTED_KIND, message, Map.of("kind", kind.value()));
        this.unsupportedKind = kind;
    }

    public SpaceKind kind() {
        return unsupportedKind;
    }
}
