package yeobaek.backend.appreciation.internal;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.appreciation.api.AppreciationRootApi;
import yeobaek.backend.appreciation.api.AppreciationNotFoundFailure;
import yeobaek.backend.appreciation.persistence.AppreciationJpaEntity;
import yeobaek.backend.appreciation.persistence.AppreciationRepository;
import yeobaek.backend.foundation.identity.AppreciationId;
import yeobaek.backend.foundation.identity.MemberId;

@Service
@RequiredArgsConstructor
@Transactional
public class AppreciationRootService implements AppreciationRootApi {

    private final AppreciationRepository repository;

    @Override
    public Root create(String kind, MemberId authorId, LocalDateTime createdAt) {
        return toRoot(repository.save(new AppreciationJpaEntity(kind, authorId.value(), createdAt)));
    }

    @Override
    @Transactional(readOnly = true)
    public Root get(AppreciationId appreciationId) {
        return toRoot(find(appreciationId));
    }

    @Override
    public Root getForUpdate(AppreciationId appreciationId) {
        return toRoot(repository.findByIdForUpdate(appreciationId.value())
                .orElseThrow(() -> notFound(appreciationId)));
    }

    @Override
    public void markUpdated(AppreciationId appreciationId, LocalDateTime updatedAt) {
        find(appreciationId).markUpdated(updatedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Root> findAuthoredBy(MemberId authorId) {
        return repository.findAllByAuthorId(authorId.value()).stream().map(this::toRoot).toList();
    }

    @Override
    public void delete(AppreciationId appreciationId) {
        repository.deleteById(appreciationId.value());
    }

    @Override
    public void eraseAuthoredBy(MemberId authorId) {
        repository.deleteAllByAuthorId(authorId.value());
    }

    private AppreciationJpaEntity find(AppreciationId appreciationId) {
        return repository.findById(appreciationId.value()).orElseThrow(() -> notFound(appreciationId));
    }

    private AppreciationNotFoundFailure notFound(AppreciationId appreciationId) {
        return new AppreciationNotFoundFailure(appreciationId);
    }

    private Root toRoot(AppreciationJpaEntity entity) {
        return new Root(new AppreciationId(entity.getId()), new MemberId(entity.getAuthorId()), entity.getKind(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
