package yeobaek.backend.appreciation.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;

@FunctionalInterface
public interface CommentQueryApi {

    Map<ContentLocationId, Long> countByLocation(
            SpaceId spaceId, List<ContentLocationId> locationIds, Set<MemberId> excludedAuthorIds);
}
