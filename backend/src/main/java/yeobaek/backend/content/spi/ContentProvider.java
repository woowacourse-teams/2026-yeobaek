package yeobaek.backend.content.spi;

import yeobaek.backend.content.api.Content;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;

public interface ContentProvider {

    ContentKind supportedKind();

    Content get(ContentId contentId);
}
