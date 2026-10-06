package yeobaek.backend.collaboration.spi.binding;

import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;

public interface SpaceContentBindingCapability {

    SpaceKind supportedKind();

    void bind(SpaceId spaceId, ContentId contentId);
}
