package yeobaek.backend.appreciation.api;

import java.util.List;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

@FunctionalInterface
public interface CommentViewApi {

    void markViewed(MemberId actorId, List<AppreciationId> commentIds);
}
