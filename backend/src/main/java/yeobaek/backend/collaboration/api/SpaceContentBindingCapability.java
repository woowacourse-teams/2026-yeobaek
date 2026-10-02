package yeobaek.backend.collaboration.api;

import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.SpaceId;

public interface SpaceContentBindingCapability {

    String supportedKind();

    void bind(SpaceId spaceId, ContentId contentId);
}
