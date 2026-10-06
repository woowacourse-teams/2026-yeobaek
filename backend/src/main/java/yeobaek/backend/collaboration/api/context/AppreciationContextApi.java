package yeobaek.backend.collaboration.api.context;

import java.util.List;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.SpaceId;

public interface AppreciationContextApi {

    void attach(AppreciationId appreciationId, SpaceId spaceId, ContentId contentId,
                ContentLocationId locationId);

    Context get(AppreciationId appreciationId);

    void detach(AppreciationId appreciationId);

    void detachAll(List<AppreciationId> appreciationIds);

    List<LocatedAppreciation> findInSpace(SpaceId spaceId);

    record Context(SpaceId spaceId, ContentId contentId, ContentLocationId locationId) {
    }

    record LocatedAppreciation(AppreciationId appreciationId, ContentId contentId, ContentLocationId locationId) {
    }
}
