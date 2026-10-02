package yeobaek.backend.appreciation.api;

import yeobaek.backend.foundation.identity.AppreciationId;

public final class CommentFailure extends RuntimeException {

    public enum Reason {
        NOT_FOUND,
        NOT_OWNER,
        CANNOT_REPORT_OWN_COMMENT,
        NOT_VISIBLE
    }

    private final transient Reason failureReason;
    private final long failedCommentId;

    public CommentFailure(Reason reason, AppreciationId commentId) {
        this(reason, commentId, null);
    }

    public CommentFailure(Reason reason, AppreciationId commentId, Throwable cause) {
        super(message(reason, commentId), cause);
        this.failureReason = reason;
        this.failedCommentId = commentId.value();
    }

    public Reason reason() {
        return failureReason;
    }

    public long commentId() {
        return failedCommentId;
    }

    private static String message(Reason reason, AppreciationId commentId) {
        return "댓글 작업을 수행할 수 없습니다: reason=" + reason + ", commentId=" + commentId.value();
    }
}
