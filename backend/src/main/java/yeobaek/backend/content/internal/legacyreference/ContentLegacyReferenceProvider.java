package yeobaek.backend.content.internal.legacyreference;

import java.util.Collection;
import java.util.Map;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;

public interface ContentLegacyReferenceProvider {

    ContentKind supportedKind();

    ContentId resolve(long legacyId);

    Map<ContentId, Long> legacyIds(Collection<ContentId> contentIds);
}
