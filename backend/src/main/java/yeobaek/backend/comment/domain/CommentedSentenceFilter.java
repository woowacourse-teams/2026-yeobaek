package yeobaek.backend.comment.domain;

public enum CommentedSentenceFilter {
    ALL,
    NEW;

    public boolean matches(CommentedSentence sentence) {
        return this == ALL || sentence.unreadCommentCount() > 0;
    }
}
