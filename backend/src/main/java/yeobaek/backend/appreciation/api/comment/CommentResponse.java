package yeobaek.backend.appreciation.api.comment;

import yeobaek.backend.appreciation.api.Appreciation;
import yeobaek.backend.shared.identity.MemberId;

public interface CommentResponse extends Appreciation {

    String content();

    default boolean isWrittenBy(MemberId actorId) {
        return authorId().equals(actorId);
    }
}
