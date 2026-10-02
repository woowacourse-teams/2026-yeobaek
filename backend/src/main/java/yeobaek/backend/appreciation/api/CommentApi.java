package yeobaek.backend.appreciation.api;

import java.util.List;
import yeobaek.backend.appreciation.domain.Comment;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

public interface CommentApi {

    Comment create(MemberId authorId, String content);

    Comment get(AppreciationId commentId);

    Comment getForUpdate(AppreciationId commentId);

    Comment update(MemberId actorId, AppreciationId commentId, String content);

    void delete(MemberId actorId, AppreciationId commentId);

    void markViewed(MemberId actorId, List<AppreciationId> commentIds);

    boolean report(MemberId reporterId, AppreciationId commentId);
}
