package yeobaek.backend.content.internal;

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.idmapping.ContentIdMappingApi;
import yeobaek.backend.content.internal.idmapping.ContentIdMappingProvider;
import yeobaek.backend.shared.identity.ContentId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentIdMappingService implements ContentIdMappingApi {

    private final Map<ContentKind, ContentIdMappingProvider> providers;

    @Override
    public ContentId toContentId(ContentKind kind, long implementationId) {
        return provider(kind).toContentId(implementationId);
    }

    @Override
    public Map<ContentId, Long> toImplementationIds(ContentKind kind, Collection<ContentId> contentIds) {
        return provider(kind).toImplementationIds(contentIds);
    }

    private ContentIdMappingProvider provider(ContentKind kind) {
        ContentIdMappingProvider provider = providers.get(kind);
        if (provider == null) {
            throw new IllegalArgumentException("지원하지 않는 capability입니다: " + kind);
        }
        return provider;
    }
}
