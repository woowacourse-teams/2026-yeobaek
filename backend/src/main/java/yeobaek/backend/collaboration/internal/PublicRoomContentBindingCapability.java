package yeobaek.backend.collaboration.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.collaboration.api.SpaceContentBindingCapability;
import yeobaek.backend.collaboration.persistence.PublicRoomContentBindingJpaEntity;
import yeobaek.backend.collaboration.persistence.PublicRoomContentBindingRepository;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.domain.PublicRoom;

@Component
@RequiredArgsConstructor
public class PublicRoomContentBindingCapability implements SpaceContentBindingCapability {

    private final PublicRoomContentBindingRepository repository;

    @Override
    public String supportedKind() {
        return PublicRoom.KIND;
    }

    @Override
    public void bind(SpaceId spaceId, ContentId contentId) {
        repository.save(new PublicRoomContentBindingJpaEntity(spaceId.value(), contentId.value()));
    }
}
