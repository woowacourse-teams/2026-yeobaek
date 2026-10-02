package yeobaek.backend.collaboration.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.collaboration.api.SpaceContentBindingCapability;
import yeobaek.backend.collaboration.persistence.ClubContentBindingJpaEntity;
import yeobaek.backend.collaboration.persistence.ClubContentBindingRepository;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.domain.Club;

@Component
@RequiredArgsConstructor
public class ClubContentBindingCapability implements SpaceContentBindingCapability {

    private final ClubContentBindingRepository repository;

    @Override
    public String supportedKind() {
        return Club.KIND;
    }

    @Override
    public void bind(SpaceId spaceId, ContentId contentId) {
        repository.save(new ClubContentBindingJpaEntity(spaceId.value(), contentId.value()));
    }
}
