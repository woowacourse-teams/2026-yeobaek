package yeobaek.backend.appreciation.internal.erasure;

import java.util.Map;
import yeobaek.backend.appreciation.api.AppreciationKind;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class UnsupportedAppreciationKindException extends LogContextException {

    public UnsupportedAppreciationKindException(AppreciationKind kind, String message, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message, Map.of("kind", kind.value()), cause);
    }
}
