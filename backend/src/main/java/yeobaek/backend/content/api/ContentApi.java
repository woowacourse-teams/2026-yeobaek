package yeobaek.backend.content.api;

import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

public interface ContentApi {

    Content getContent(ContentId contentId);

    void requireAvailable(ContentId contentId);

    boolean ownsLocation(ContentId contentId, ContentLocationId locationId);
}
