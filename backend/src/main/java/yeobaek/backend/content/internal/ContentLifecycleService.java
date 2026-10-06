package yeobaek.backend.content.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.lifecycle.ContentLifecycleApi;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.content.api.ContentNotFoundException;
import yeobaek.backend.content.persistence.ContentJpaEntity;
import yeobaek.backend.content.persistence.ContentLocationJpaEntity;
import yeobaek.backend.content.persistence.ContentLocationRepository;
import yeobaek.backend.content.persistence.ContentRootRepository;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;

@Service
@RequiredArgsConstructor
public class ContentLifecycleService implements ContentLifecycleApi {

    private final ContentRootRepository contentRepository;
    private final ContentLocationRepository locationRepository;

    @Override
    @Transactional
    public ContentId createContent(ContentKind kind) {
        ContentJpaEntity content = contentRepository.save(new ContentJpaEntity(kind));
        return new ContentId(content.getId());
    }

    @Override
    @Transactional
    public ContentLocationId createLocation(ContentId contentId, LocationKind kind) {
        ContentJpaEntity content = contentRepository.findById(contentId.value())
                .orElseThrow(() -> new ContentNotFoundException(contentId,
                        "삭제할 컨텐츠가 존재하지 않습니다: contentId=" + contentId.value()));
        ContentLocationJpaEntity location = locationRepository.save(new ContentLocationJpaEntity(content, kind));
        return new ContentLocationId(location.getId());
    }
}
