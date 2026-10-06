package yeobaek.backend.collaboration.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.collaboration.internal.binding.SpaceContentBindingCapability;
import yeobaek.backend.collaboration.persistence.SpaceContentBindingJpaEntity;
import yeobaek.backend.collaboration.persistence.SpaceContentBindingRepository;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.access.SpaceAccessApi;
import yeobaek.backend.space.api.SpaceKind;

@Service
@RequiredArgsConstructor
public class SpaceContentBindingService implements SpaceContentBindingApi {

    private final SpaceContentBindingRepository bindingRepository;
    private final Map<SpaceKind, SpaceContentBindingCapability> capabilities;
    private final SpaceAccessApi spaceAccessApi;

    @Override
    @Transactional
    public void bind(SpaceId spaceId, ContentId contentId) {
        SpaceKind spaceKind = spaceAccessApi.getSpace(spaceId).kind();
        bindingRepository.save(new SpaceContentBindingJpaEntity(spaceId.value(), contentId.value()));
        capability(spaceKind).bind(spaceId, contentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBound(SpaceId spaceId, ContentId contentId) {
        return bindingRepository.existsById(new yeobaek.backend.collaboration.persistence.SpaceContentBindingId(
                spaceId.value(), contentId.value()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentId> findContents(SpaceId spaceId) {
        return bindingRepository.findAllBySpaceIdOrderByContentId(spaceId.value()).stream()
                .map(binding -> new ContentId(binding.getContentId())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpaceId> findSpaces(ContentId contentId) {
        return bindingRepository.findAllByContentIdOrderBySpaceId(contentId.value()).stream()
                .map(binding -> new SpaceId(binding.getSpaceId())).toList();
    }

    private SpaceContentBindingCapability capability(SpaceKind kind) {
        SpaceContentBindingCapability capability = capabilities.get(kind);
        if (capability == null) {
            throw new IllegalArgumentException("지원하지 않는 capability입니다: " + kind);
        }
        return capability;
    }
}
