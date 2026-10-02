package yeobaek.backend.collaboration.api;

import java.util.List;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.SpaceId;

public interface SpaceContentBindingApi {

    void bind(SpaceId spaceId, ContentId contentId);

    boolean isBound(SpaceId spaceId, ContentId contentId);

    List<ContentId> findContents(SpaceId spaceId);
}
