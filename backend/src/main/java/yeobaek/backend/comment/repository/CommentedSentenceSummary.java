package yeobaek.backend.comment.repository;

import java.time.LocalDateTime;

public interface CommentedSentenceSummary {

    Long getSentenceId();

    String getContent();

    Long getPassageId();

    int getPassageSequence();

    int getSentenceSequence();

    long getCommentCount();

    long getUnreadCommentCount();

    LocalDateTime getLatestCommentCreatedAt();
}
