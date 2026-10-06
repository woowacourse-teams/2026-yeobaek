package yeobaek.backend.space.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.lifecycle.SpaceRootLifecycleApi;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.persistence.SpaceJpaEntity;
import yeobaek.backend.space.persistence.SpaceRootRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class SpaceRootLifecycleService implements SpaceRootLifecycleApi {

    private final SpaceRootRepository repository;

    @Override
    public SpaceId create(SpaceKind kind) {
        return new SpaceId(repository.save(new SpaceJpaEntity(kind)).getId());
    }
}
