package yeobaek.backend.collaboration.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.collaboration.api.AppreciationContextApi;
import yeobaek.backend.collaboration.persistence.AppreciationContextJpaEntity;
import yeobaek.backend.collaboration.persistence.AppreciationContextRepository;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;

@Service
@RequiredArgsConstructor
@Transactional
public class AppreciationContextService implements AppreciationContextApi {

    private final AppreciationContextRepository repository;

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
    public void eraseAuthoredBy(MemberId authorId) {
        repository.deleteAllByAuthorId(authorId.value());
    }
}
