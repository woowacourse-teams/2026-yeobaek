package yeobaek.backend.collaboration.api.binding;

import java.util.List;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;

public interface SpaceContentBindingApi {

    void bind(SpaceId spaceId, ContentId contentId);

    boolean isBound(SpaceId spaceId, ContentId contentId);

    List<ContentId> findContents(SpaceId spaceId);

    List<SpaceId> findSpaces(ContentId contentId);
}
