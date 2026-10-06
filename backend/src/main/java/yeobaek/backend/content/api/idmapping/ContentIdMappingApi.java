package yeobaek.backend.content.api.idmapping;

import java.util.Collection;
import java.util.Map;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.shared.identity.ContentId;

/** Maps implementation-specific identifiers, currently {@code bookId}, to and from shared {@link ContentId}s. */
public interface ContentIdMappingApi {

    ContentId toContentId(ContentKind kind, long implementationId);

    Map<ContentId, Long> toImplementationIds(ContentKind kind, Collection<ContentId> contentIds);
}
