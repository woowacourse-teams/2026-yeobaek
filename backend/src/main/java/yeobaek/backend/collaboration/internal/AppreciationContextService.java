package yeobaek.backend.collaboration.internal;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.collaboration.api.context.AppreciationContextApi;
import yeobaek.backend.collaboration.persistence.AppreciationContextJpaEntity;
import yeobaek.backend.collaboration.persistence.AppreciationContextRepository;
import yeobaek.backend.shared.identity.AppreciationId;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.SpaceId;

@Service
@RequiredArgsConstructor
@Transactional
public class AppreciationContextService implements AppreciationContextApi {

    private final AppreciationContextRepository repository;
    private final EntityManager entityManager;

    @Override
    public void attach(AppreciationId appreciationId, SpaceId spaceId, ContentId contentId,
                       ContentLocationId locationId) {
        repository.save(new AppreciationContextJpaEntity(
                appreciationId.value(), spaceId.value(), contentId.value(), locationId.value()));
    }

    @Override
    @Transactional(readOnly = true)
    public Context get(AppreciationId appreciationId) {
        var context = repository.findById(appreciationId.value()).orElseThrow();
        return new Context(new SpaceId(context.getSpaceId()), new ContentId(context.getContentId()),
                new ContentLocationId(context.getLocationId()));
    }

    @Override
    public void detach(AppreciationId appreciationId) {
        repository.deleteById(appreciationId.value());
    }

    @Override
    public void detachAll(List<AppreciationId> appreciationIds) {
        if (!appreciationIds.isEmpty()) {
            repository.deleteAllByIdInBatch(appreciationIds.stream().map(AppreciationId::value).toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocatedAppreciation> findInSpace(SpaceId spaceId) {
        return entityManager.createQuery("""
                select context from AppreciationContextJpaEntity context where context.spaceId = :spaceId
                """, AppreciationContextJpaEntity.class)
                .setParameter("spaceId", spaceId.value()).getResultStream()
                .map(context -> new LocatedAppreciation(new AppreciationId(context.getAppreciationId()),
                        new ContentId(context.getContentId()), new ContentLocationId(context.getLocationId())))
                .toList();
    }
}
