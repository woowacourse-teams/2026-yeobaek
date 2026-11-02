package yeobaek.backend.comment.domain;

public enum CommentedSentenceFilter {
    ALL,
    NEW;

    public boolean includes(long unreadCommentCount) {
        return this == ALL || unreadCommentCount > 0;
    }
}
