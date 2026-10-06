package yeobaek.backend.appreciation.api.comment;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.MemberId;

public interface CommentViewApi {

    void markViewed(MemberId actorId, List<AppreciationId> commentIds);

    Set<AppreciationId> findViewedIds(MemberId actorId, Collection<AppreciationId> commentIds);
}
