package yeobaek.backend.content.api.book;

import java.util.Map;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;

public final class BookIngestException extends LogContextException {

    public BookIngestException(ErrorCode code, String message, Map<String, String> context) {
        super(code, message, context);
    }
}
