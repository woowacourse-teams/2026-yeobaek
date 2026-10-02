package yeobaek.backend.reading.internal;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.reading.domain.ReadingProgress;
import yeobaek.backend.reading.persistence.ReadingProgressJpaEntity;
import yeobaek.backend.reading.persistence.ReadingProgressRepository;

@Service
@RequiredArgsConstructor
public class ReadingProgressService implements ReadingProgressApi {

    private final ReadingProgressRepository repository;

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
    public void erase(MemberId actorId) {
        repository.deleteAllByActorId(actorId.value());
    }

    private ReadingProgress toDomain(ReadingProgressJpaEntity entity) {
        return new ReadingProgress(new MemberId(entity.getActorId()), new SpaceId(entity.getSpaceId()),
                new ContentId(entity.getContentId()), new ContentLocationId(entity.getLocationId()),
                entity.getLastReadAt());
    }
}
