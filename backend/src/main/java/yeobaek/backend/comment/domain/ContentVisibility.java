package yeobaek.backend.comment.domain;

public enum ContentVisibility {
    VISIBLE,
    REVEAL_REQUIRED;

    public static ContentVisibility from(boolean future, long unreadCommentCount) {
        if (future && unreadCommentCount > 0) {
            return REVEAL_REQUIRED;
        }
        return VISIBLE;
    }
}
