package yeobaek.backend.content.api.legacyreference;

import java.util.Collection;
import java.util.Map;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;

public interface ContentLegacyReferenceApi {

    ContentId resolve(ContentKind kind, long legacyId);

    Map<ContentId, Long> legacyIds(ContentKind kind, Collection<ContentId> contentIds);
}
