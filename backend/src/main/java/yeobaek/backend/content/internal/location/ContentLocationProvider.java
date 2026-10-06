package yeobaek.backend.content.internal.location;

import java.util.Optional;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.shared.identity.ContentLocationId;

public interface ContentLocationProvider {

    ContentKind supportedKind();

    Optional<PassageContext> findPassageContext(ContentLocationId locationId, LocationKind locationKind);

    record PassageContext(ContentLocationId locationId, int sequence) {
    }
}
