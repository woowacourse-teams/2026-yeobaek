package yeobaek.backend.reading.internal;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.reading.api.ReadingProgress;
import yeobaek.backend.reading.persistence.ReadingProgressJpaEntity;
import yeobaek.backend.reading.persistence.ReadingProgressRepository;

@Service
@RequiredArgsConstructor
public class ReadingProgressService implements ReadingProgressApi {

    private final ReadingProgressRepository repository;
    private final EntityManager entityManager;

    @Override
    public ReadingProgress update(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                  ContentLocationId locationId, LocalDateTime readAt) {
        ReadingProgressJpaEntity progress = repository.findOne(actorId.value(), spaceId.value(), contentId.value())
                .orElseGet(() -> new ReadingProgressJpaEntity(
                        actorId.value(), spaceId.value(), contentId.value(), locationId.value(), readAt));
        progress.update(locationId.value(), readAt);
        return toDomain(repository.save(progress));
    }

    @Override
    public Optional<ReadingProgress> find(MemberId actorId, SpaceId spaceId, ContentId contentId) {
        return repository.findOne(actorId.value(), spaceId.value(), contentId.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingProgress> findByActor(MemberId actorId) {
        return entityManager.createQuery("""
                select progress from ReadingProgressJpaEntity progress
                where progress.actorId = :actorId order by progress.lastReadAt desc
                """, ReadingProgressJpaEntity.class)
                .setParameter("actorId", actorId.value()).getResultStream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingProgress> findBySpaces(MemberId actorId, List<SpaceId> spaceIds) {
        if (spaceIds.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("""
                select progress from ReadingProgressJpaEntity progress
                where progress.actorId = :actorId and progress.spaceId in :spaceIds
                """, ReadingProgressJpaEntity.class)
                .setParameter("actorId", actorId.value())
                .setParameter("spaceIds", spaceIds.stream().map(SpaceId::value).toList())
                .getResultStream().map(this::toDomain).toList();
    }

    @Override
    public void erase(MemberId actorId) {
        repository.deleteAllByActorId(actorId.value());
    }

    private ReadingProgress toDomain(ReadingProgressJpaEntity entity) {
        return new ReadingProgress(new MemberId(entity.getActorId()), new SpaceId(entity.getSpaceId()),
                new ContentId(entity.getContentId()), new ContentLocationId(entity.getLocationId()),
                entity.getLastReadAt());
    }
}
