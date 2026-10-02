package yeobaek.backend.content.api;

import yeobaek.backend.content.domain.Content;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;

public interface ContentApi {

    Content getContent(ContentId contentId);

    void requireAvailable(ContentId contentId);

    boolean ownsLocation(ContentId contentId, ContentLocationId locationId);
}
