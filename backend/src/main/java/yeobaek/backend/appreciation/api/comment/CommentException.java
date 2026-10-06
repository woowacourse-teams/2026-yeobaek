package yeobaek.backend.appreciation.api.comment;

import java.util.Map;
import java.util.HashMap;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.exception.LogContextException;
import yeobaek.backend.shared.identity.AppreciationId;

public final class CommentException extends LogContextException {

    private final long failedCommentId;

    public CommentException(ErrorCode code, AppreciationId commentId, String message) {
        this(code, commentId, message, Map.of(), null);
    }

    public CommentException(ErrorCode code, AppreciationId commentId, String message, Throwable cause) {
        this(code, commentId, message, Map.of(), cause);
    }

    public CommentException(ErrorCode code, AppreciationId commentId, String message,
                            Map<String, String> logContext) {
        this(code, commentId, message, logContext, null);
    }

    public CommentException(ErrorCode code, AppreciationId commentId, String message,
                            Map<String, String> logContext, Throwable cause) {
        super(code, message, context(commentId, logContext), cause);
        this.failedCommentId = commentId.value();
    }

    public long commentId() {
        return failedCommentId;
    }

    private static Map<String, String> context(AppreciationId commentId, Map<String, String> logContext) {
        Map<String, String> context = new HashMap<>(logContext);
        context.put("commentId", Long.toString(commentId.value()));
        return context;
    }
}
