package yeobaek.backend.content.api.location;

import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

public interface ContentLocationQueryApi {

    Location get(ContentLocationId locationId);

    record Location(
            ContentLocationId id,
            ContentId contentId,
            LocationKind kind,
            ContentLocationId passageLocationId,
            int passageSequence
    ) {
    }
}
