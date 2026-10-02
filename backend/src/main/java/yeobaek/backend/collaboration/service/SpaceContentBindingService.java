package yeobaek.backend.collaboration.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.collaboration.api.SpaceContentBindingRegistry;
import yeobaek.backend.collaboration.persistence.SpaceContentBindingJpaEntity;
import yeobaek.backend.collaboration.persistence.SpaceContentBindingRepository;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.api.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class SpaceContentBindingService implements SpaceContentBindingApi {

    private final SpaceContentBindingRepository bindingRepository;
    private final SpaceContentBindingRegistry capabilities;
    private final SpaceAccessApi spaceAccessApi;

    @Override
    @Transactional
    public void bind(SpaceId spaceId, ContentId contentId) {
        String spaceKind = spaceAccessApi.getSpace(spaceId).kind();
        bindingRepository.save(new SpaceContentBindingJpaEntity(spaceId.value(), contentId.value()));
        capabilities.get(spaceKind).bind(spaceId, contentId);
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
}
