package yeobaek.backend.collaboration.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.collaboration.internal.binding.SpaceContentBindingCapability;
import yeobaek.backend.collaboration.persistence.PublicRoomContentBindingJpaEntity;
import yeobaek.backend.collaboration.persistence.PublicRoomContentBindingRepository;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;

@Component
@RequiredArgsConstructor
public class PublicRoomContentBindingCapability implements SpaceContentBindingCapability {

    private final PublicRoomContentBindingRepository repository;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.PUBLIC_ROOM;
    }

    @Override
    public void bind(SpaceId spaceId, ContentId contentId) {
        repository.save(new PublicRoomContentBindingJpaEntity(spaceId.value(), contentId.value()));
    }
}
