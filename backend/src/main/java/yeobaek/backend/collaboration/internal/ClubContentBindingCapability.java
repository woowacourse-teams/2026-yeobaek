package yeobaek.backend.collaboration.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.collaboration.spi.binding.SpaceContentBindingCapability;
import yeobaek.backend.collaboration.persistence.ClubContentBindingJpaEntity;
import yeobaek.backend.collaboration.persistence.ClubContentBindingRepository;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;

@Component
@RequiredArgsConstructor
public class ClubContentBindingCapability implements SpaceContentBindingCapability {

    private final ClubContentBindingRepository repository;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.CLUB;
    }

    @Override
    public void bind(SpaceId spaceId, ContentId contentId) {
        repository.save(new ClubContentBindingJpaEntity(spaceId.value(), contentId.value()));
    }
}
