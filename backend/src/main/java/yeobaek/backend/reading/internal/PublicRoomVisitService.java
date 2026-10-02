package yeobaek.backend.reading.internal;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.reading.api.PublicRoomVisitApi;
import yeobaek.backend.reading.domain.PublicRoomVisit;
import yeobaek.backend.reading.persistence.PublicRoomVisitJpaEntity;
import yeobaek.backend.reading.persistence.PublicRoomVisitRepository;

@Service
@RequiredArgsConstructor
public class PublicRoomVisitService implements PublicRoomVisitApi {

    private final PublicRoomVisitRepository repository;

    @Override
    public PublicRoomVisit visit(MemberId actorId, SpaceId spaceId, LocalDateTime visitedAt) {
        PublicRoomVisitJpaEntity visit = repository.findOne(actorId.value(), spaceId.value())
                .orElseGet(() -> new PublicRoomVisitJpaEntity(actorId.value(), spaceId.value()));
        visit.visit(visitedAt);
        return toDomain(repository.save(visit));
    }

    @Override
    public void erase(MemberId actorId) {
        repository.deleteAllByActorId(actorId.value());
    }

    private PublicRoomVisit toDomain(PublicRoomVisitJpaEntity entity) {
        return new PublicRoomVisit(new MemberId(entity.getActorId()), new SpaceId(entity.getSpaceId()),
                entity.getLastVisitedAt());
    }
}
