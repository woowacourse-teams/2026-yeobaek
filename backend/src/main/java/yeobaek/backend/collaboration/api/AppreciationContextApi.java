package yeobaek.backend.collaboration.api;

import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.SpaceId;

public interface AppreciationContextApi {

    void attach(AppreciationId appreciationId, SpaceId spaceId, ContentId contentId,
                ContentLocationId locationId);

    Context get(AppreciationId appreciationId);

    void detach(AppreciationId appreciationId);

    void eraseAuthoredBy(MemberId authorId);

    record Context(SpaceId spaceId, ContentId contentId, ContentLocationId locationId) {
    }
}
