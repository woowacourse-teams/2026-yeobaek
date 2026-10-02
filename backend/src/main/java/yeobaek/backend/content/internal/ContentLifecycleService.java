package yeobaek.backend.content.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.ContentLifecycleApi;
import yeobaek.backend.content.api.ContentNotFoundFailure;
import yeobaek.backend.content.persistence.ContentJpaEntity;
import yeobaek.backend.content.persistence.ContentLocationJpaEntity;
import yeobaek.backend.content.persistence.ContentLocationRepository;
import yeobaek.backend.content.persistence.ContentRootRepository;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;

@Service
@RequiredArgsConstructor
public class ContentLifecycleService implements ContentLifecycleApi {

    private final ContentRootRepository contentRepository;
    private final ContentLocationRepository locationRepository;

    @Override
    @Transactional
    public ContentId createContent(String kind) {
        ContentJpaEntity content = contentRepository.save(new ContentJpaEntity(kind));
        return new ContentId(content.getId());
    }

    @Override
    @Transactional
    public ContentLocationId createLocation(ContentId contentId, String kind) {
        ContentJpaEntity content = contentRepository.findById(contentId.value())
                .orElseThrow(() -> new ContentNotFoundFailure(contentId));
        ContentLocationJpaEntity location = locationRepository.save(new ContentLocationJpaEntity(content, kind));
        return new ContentLocationId(location.getId());
    }
}
