package yeobaek.backend.content.internal;

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.legacyreference.ContentLegacyReferenceApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.spi.legacyreference.ContentLegacyReferenceProviderRegistry;
import yeobaek.backend.shared.identity.ContentId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentLegacyReferenceService implements ContentLegacyReferenceApi {

    private final ContentLegacyReferenceProviderRegistry providers;

    @Override
    public ContentId resolve(ContentKind kind, long legacyId) {
        return providers.get(kind).resolve(legacyId);
    }

    @Override
    public Map<ContentId, Long> legacyIds(ContentKind kind, Collection<ContentId> contentIds) {
        return providers.get(kind).legacyIds(contentIds);
    }
}
