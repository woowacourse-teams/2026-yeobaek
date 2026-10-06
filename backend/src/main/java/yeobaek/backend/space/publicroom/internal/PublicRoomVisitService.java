package yeobaek.backend.space.publicroom.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitApi;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitResponse;
import yeobaek.backend.space.publicroom.persistence.PublicRoomVisitJpaEntity;
import yeobaek.backend.space.publicroom.persistence.PublicRoomVisitRepository;
import yeobaek.backend.space.publicroom.persistence.PublicRoomVisitorCountView;

@Service
@RequiredArgsConstructor
public class PublicRoomVisitService implements PublicRoomVisitApi {

    private final PublicRoomVisitRepository repository;

    @Override
    public PublicRoomVisitResponse visit(MemberId actorId, SpaceId spaceId, LocalDateTime visitedAt) {
        PublicRoomVisitJpaEntity visit = repository.findOne(actorId.value(), spaceId.value())
                .orElseGet(() -> new PublicRoomVisitJpaEntity(actorId.value(), spaceId.value()));
        visit.visit(visitedAt);
        return toResponse(repository.save(visit));
    }

    @Override
    public void erase(MemberId actorId) {
        repository.deleteAllByActorId(actorId.value());
    }

    @Override
    public List<PublicRoomVisitResponse> findVisits(MemberId actorId) {
        return repository.findAllByActorIdOrderByLastVisitedAtDesc(actorId.value()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Optional<PublicRoomVisitResponse> findLastVisit(MemberId actorId, SpaceId spaceId) {
        return repository.findOne(actorId.value(), spaceId.value()).map(this::toResponse);
    }

    @Override
    public Map<SpaceId, Long> countVisitors(List<SpaceId> spaceIds) {
        if (spaceIds.isEmpty()) {
            return Map.of();
        }
        return repository.countVisitors(spaceIds.stream().map(SpaceId::value).toList()).stream()
                .collect(Collectors.toUnmodifiableMap(
                        view -> new SpaceId(view.getSpaceId()),
                        PublicRoomVisitorCountView::getVisitorCount));
    }

    private PublicRoomVisitResponse toResponse(PublicRoomVisitJpaEntity entity) {
        return new PublicRoomVisitResponse(new MemberId(entity.getActorId()), new SpaceId(entity.getSpaceId()),
                entity.getLastVisitedAt());
    }
}
