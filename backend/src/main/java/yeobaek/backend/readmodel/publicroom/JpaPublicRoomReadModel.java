package yeobaek.backend.readmodel.publicroom;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import yeobaek.backend.book.domain.Passage;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.reading.persistence.PublicRoomVisitRepository;
import yeobaek.backend.reading.persistence.ReadingProgressJpaEntity;
import yeobaek.backend.reading.persistence.ReadingProgressRepository;
import yeobaek.backend.reading.persistence.PublicRoomVisitJpaEntity;
import yeobaek.backend.readmodel.book.SpaceBookReadModel;

@Component
@RequiredArgsConstructor
public class JpaPublicRoomReadModel implements PublicRoomReadModel {

    private final PublicRoomRepository publicRoomRepository;
    private final ReadingProgressRepository progressRepository;
    private final PublicRoomVisitRepository visitRepository;
    private final PassageRepository passageRepository;
    private final SpaceBookReadModel bookReadModel;
    private final EntityManager entityManager;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<RoomSnapshot> findAllActive() {
        return publicRoomRepository.findAll().stream().map(this::toRoom)
                .filter(room -> room.book().available()).toList();
    }

    @Override
    public Optional<RoomSnapshot> findRoom(Long publicRoomId) {
        return publicRoomRepository.findById(publicRoomId).map(this::toRoom);
    }

    @Override
    public Optional<RoomSnapshot> findRoomForUpdate(Long publicRoomId) {
        return publicRoomRepository.findByIdForUpdate(publicRoomId).map(this::toRoom);
    }

    @Override
    public List<VisitedRoomSnapshot> findVisitedActive(MemberId actorId) {
        return entityManager.createQuery("""
                select v from PublicRoomVisitJpaEntity v
                join PublicRoom room on room.spaceId = v.spaceId
                join SpaceContentBindingJpaEntity binding on binding.spaceId = v.spaceId
                join Book book on book.contentId = binding.contentId
                where v.actorId = :actorId and v.lastVisitedAt is not null
                  and book.status = yeobaek.backend.book.domain.BookStatus.ACTIVE
                order by v.lastVisitedAt desc
                """, PublicRoomVisitJpaEntity.class).setParameter("actorId", actorId.value())
                .getResultStream().map(visit -> {
            RoomSnapshot room = publicRoomRepository.findBySpaceRootId(visit.getSpaceId())
                    .map(this::toRoom).orElseThrow();
            ProgressSnapshot progress = progressRepository.findOne(actorId.value(), room.space().id().value(),
                    room.book().contentId().value()).map(this::toProgress).orElse(null);
            return new VisitedRoomSnapshot(room, progress, visit.getLastVisitedAt());
        }).toList();
    }

    @Override
    public Map<SpaceId, ProgressSnapshot> findProgresses(MemberId actorId, List<SpaceId> spaceIds) {
        if (spaceIds.isEmpty()) {
            return Map.of();
        }
        return entityManager.createQuery("""
                select p from ReadingProgressJpaEntity p
                where p.actorId = :actorId and p.spaceId in :spaceIds
                """, ReadingProgressJpaEntity.class).setParameter("actorId", actorId.value())
                .setParameter("spaceIds", spaceIds.stream().map(SpaceId::value).toList()).getResultStream()
                .collect(Collectors.toMap(progress -> new SpaceId(progress.getSpaceId()), this::toProgress));
    }

    @Override
    public Optional<ProgressSnapshot> findProgress(MemberId actorId, SpaceId spaceId, ContentId contentId) {
        return progressRepository.findOne(actorId.value(), spaceId.value(), contentId.value()).map(this::toProgress);
    }

    @Override
    public Optional<LocalDateTime> findLastVisit(MemberId actorId, SpaceId spaceId) {
        return visitRepository.findOne(actorId.value(), spaceId.value()).map(visit -> visit.getLastVisitedAt());
    }

    @Override
    public Map<SpaceId, Long> countVisitors(List<SpaceId> spaceIds) {
        if (spaceIds.isEmpty()) {
            return Map.of();
        }
        return visitRepository.countVisitors(spaceIds.stream().map(SpaceId::value).toList()).stream()
                .collect(Collectors.toMap(count -> new SpaceId(count.getSpaceId()), count -> count.getVisitorCount()));
    }

    @Override
    public List<PassageSnapshot> findPassages(Long bookId, int from, int to) {
        return passageRepository.findRangeByBookId(bookId, from, to).stream().map(passage ->
                new PassageSnapshot(passage.getId(), passage.getSequence().value(), passage.getChapter().getId(),
                        passage.getSentences().stream().map(sentence -> new SentenceSnapshot(
                                sentence.getId(), sentence.getSequence().value(), sentence.getContent())).toList()))
                .toList();
    }

    @Override
    public Optional<PassageLocationSnapshot> findPassage(Long passageId) {
        return passageRepository.findById(passageId).map(passage -> new PassageLocationSnapshot(
                passage.getId(), passage.getBook().getId(), new ContentLocationId(passage.getLocationId()),
                passage.getSequence().value()));
    }

    @Override
    public Map<Long, Long> countVisibleComments(MemberId actorId, Long publicRoomId, List<Long> sentenceIds) {
        if (sentenceIds.isEmpty()) {
            return Map.of();
        }
        var parameters = new MapSqlParameterSource()
                .addValue("actorId", actorId.value())
                .addValue("publicRoomId", publicRoomId)
                .addValue("sentenceIds", sentenceIds);
        return jdbcTemplate.query("""
                select s.id as sentence_id, count(c.id) as comment_count
                from comments c join appreciations a on a.id = c.id
                join appreciation_contexts ctx on ctx.appreciation_id = c.id
                join public_rooms r on r.space_id = ctx.space_id
                join sentences s on s.location_id = ctx.location_id
                where r.id = :publicRoomId and s.id in (:sentenceIds)
                  and not exists (select mb.id from member_blocks mb
                    where mb.blocker_id = :actorId and mb.blocked_id = a.author_id)
                group by s.id
                """, parameters, resultSet -> {
                    Map<Long, Long> counts = new HashMap<>();
                    while (resultSet.next()) {
                        counts.put(resultSet.getLong("sentence_id"), resultSet.getLong("comment_count"));
                    }
                    return Map.copyOf(counts);
                });
    }

    private RoomSnapshot toRoom(yeobaek.backend.publicroom.domain.PublicRoom room) {
        var book = bookReadModel.findBook(new SpaceId(room.getSpaceId())).orElseThrow();
        return new RoomSnapshot(new yeobaek.backend.space.domain.PublicRoom(
                new SpaceId(room.getSpaceId()), room.getId()),
                new BookSnapshot(book.bookId(), book.contentId(), book.title(), book.authors(), book.coverImageKey(),
                        book.passageCount(), book.status(), book.available()));
    }

    private ProgressSnapshot toProgress(ReadingProgressJpaEntity progress) {
        Passage passage = passageRepository.findByLocationId(progress.getLocationId()).orElseThrow();
        return new ProgressSnapshot(new ContentLocationId(progress.getLocationId()),
                passage.getSequence().value(), progress.getLastReadAt());
    }

}
