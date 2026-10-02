package yeobaek.backend.content.api;

import yeobaek.backend.content.domain.Content;
import yeobaek.backend.foundation.identity.ContentId;

public interface ContentProvider {

    String supportedKind();

    Content get(ContentId contentId);
}
