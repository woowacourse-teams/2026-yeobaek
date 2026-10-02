package yeobaek.backend.content.api;

import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;

public interface ContentLifecycleApi {

    ContentId createContent(String kind);

    ContentLocationId createLocation(ContentId contentId, String kind);
}
