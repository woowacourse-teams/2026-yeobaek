package yeobaek.backend.content.internal;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.ContentUnavailableException;
import yeobaek.backend.content.api.ContentNotFoundException;
import yeobaek.backend.content.spi.ContentProviderRegistry;
import yeobaek.backend.content.api.Content;
import yeobaek.backend.content.persistence.ContentRootRepository;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService implements ContentApi {

    private final ContentRootRepository rootRepository;
    private final ContentProviderRegistry providers;
    private final EntityManager entityManager;

    @Override
    public Content getContent(ContentId contentId) {
        ContentKind kind = rootRepository.findById(contentId.value())
                .orElseThrow(() -> new ContentNotFoundException(contentId,
                        "컨텐츠가 존재하지 않습니다: contentId=" + contentId.value())).contentKind();
        return providers.get(kind).get(contentId);
    }

    @Override
    public void requireAvailable(ContentId contentId) {
        if (!getContent(contentId).available()) {
            throw new ContentUnavailableException(contentId,
                    "더 이상 이용할 수 없는 컨텐츠입니다: contentId=" + contentId.value());
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
