package yeobaek.backend.content.internal.idmapping;

import java.util.Collection;
import java.util.Map;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;

public interface ContentIdMappingProvider {

    ContentKind supportedKind();

    ContentId toContentId(long implementationId);

    Map<ContentId, Long> toImplementationIds(Collection<ContentId> contentIds);
}
