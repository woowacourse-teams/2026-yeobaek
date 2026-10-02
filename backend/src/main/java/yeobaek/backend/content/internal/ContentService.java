package yeobaek.backend.content.internal;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentUnavailableFailure;
import yeobaek.backend.content.api.ContentNotFoundFailure;
import yeobaek.backend.content.api.ContentProviderRegistry;
import yeobaek.backend.content.domain.Content;
import yeobaek.backend.content.persistence.ContentRootRepository;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService implements ContentApi {

    private final ContentRootRepository rootRepository;
    private final ContentProviderRegistry providers;
    private final EntityManager entityManager;

    @Override
    public Content getContent(ContentId contentId) {
        String kind = rootRepository.findById(contentId.value())
                .orElseThrow(() -> new ContentNotFoundFailure(contentId)).getKind();
        return providers.get(kind).get(contentId);
    }

    @Override
    public void requireAvailable(ContentId contentId) {
        if (!getContent(contentId).available()) {
            throw new ContentUnavailableFailure(contentId);
        }
    }

    @Override
    public boolean ownsLocation(ContentId contentId, ContentLocationId locationId) {
        return entityManager.createQuery("""
                select count(location) from ContentLocationJpaEntity location
                where location.id = :locationId and location.content.id = :contentId
                """, Long.class)
                .setParameter("locationId", locationId.value())
                .setParameter("contentId", contentId.value())
                .getSingleResult() > 0;
    }
}
