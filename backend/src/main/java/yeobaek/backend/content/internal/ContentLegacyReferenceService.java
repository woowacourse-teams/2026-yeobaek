package yeobaek.backend.content.internal;

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.legacyreference.ContentLegacyReferenceApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.internal.legacyreference.ContentLegacyReferenceProvider;
import yeobaek.backend.shared.identity.ContentId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentLegacyReferenceService implements ContentLegacyReferenceApi {

    private final Map<ContentKind, ContentLegacyReferenceProvider> providers;

    @Override
    public ContentId resolve(ContentKind kind, long legacyId) {
        return provider(kind).resolve(legacyId);
    }

    @Override
    public Map<ContentId, Long> legacyIds(ContentKind kind, Collection<ContentId> contentIds) {
        return provider(kind).legacyIds(contentIds);
    }

    private ContentLegacyReferenceProvider provider(ContentKind kind) {
        ContentLegacyReferenceProvider provider = providers.get(kind);
        if (provider == null) {
            throw new IllegalArgumentException("지원하지 않는 capability입니다: " + kind);
        }
        return provider;
    }
}
