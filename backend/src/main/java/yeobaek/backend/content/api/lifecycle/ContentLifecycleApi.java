package yeobaek.backend.content.api.lifecycle;

import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

public interface ContentLifecycleApi {

    ContentId createContent(ContentKind kind);

    ContentLocationId createLocation(ContentId contentId, LocationKind kind);
}
