package yeobaek.backend.readmodel.reading;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import yeobaek.backend.book.repository.PassageRepository;
import yeobaek.backend.club.repository.ClubRepository;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.reading.persistence.ReadingProgressJpaEntity;
import yeobaek.backend.readmodel.book.SpaceBookReadModel;

@Component
@RequiredArgsConstructor
public class JpaRecentReadingReadModel implements RecentReadingReadModel {

    private final EntityManager entityManager;
    private final ClubRepository clubRepository;
    private final PublicRoomRepository publicRoomRepository;
    private final PassageRepository passageRepository;
    private final SpaceBookReadModel bookReadModel;

    @Override
    public List<RecentReadingSnapshot> findCandidates(MemberId actorId) {
        List<RecentReadingSnapshot> clubs = entityManager.createQuery("""
                select p from ReadingProgressJpaEntity p
                where p.actorId = :actorId and exists (
                  select cm.id from ClubMember cm where cm.memberId = :actorId
                    and cm.status = yeobaek.backend.club.domain.ClubMemberStatus.JOINED
                    and cm.club.spaceId = p.spaceId)
                order by p.lastReadAt desc
                """, ReadingProgressJpaEntity.class).setParameter("actorId", actorId.value()).getResultStream()
                .map(this::toClub).toList();
        List<RecentReadingSnapshot> rooms = entityManager.createQuery("""
                select p from ReadingProgressJpaEntity p
                where p.actorId = :actorId and exists (
                  select room.id from PublicRoom room where room.spaceId = p.spaceId)
                order by p.lastReadAt desc
                """, ReadingProgressJpaEntity.class).setParameter("actorId", actorId.value()).getResultStream()
                .map(this::toPublicRoom).toList();
        return java.util.stream.Stream.concat(clubs.stream(), rooms.stream()).toList();
    }

    private RecentReadingSnapshot toClub(ReadingProgressJpaEntity progress) {
        var club = clubRepository.findBySpaceRootId(progress.getSpaceId()).orElseThrow();
        return toSnapshot(new yeobaek.backend.space.domain.Club(new SpaceId(progress.getSpaceId()),
                club.getId(), club.getName(), club.getJoinCode()), club.getName(), progress);
    }

    private RecentReadingSnapshot toPublicRoom(ReadingProgressJpaEntity progress) {
        var room = publicRoomRepository.findBySpaceRootId(progress.getSpaceId()).orElseThrow();
        return toSnapshot(new yeobaek.backend.space.domain.PublicRoom(new SpaceId(progress.getSpaceId()),
                room.getId()), null, progress);
    }

    private RecentReadingSnapshot toSnapshot(yeobaek.backend.space.domain.Space space, String spaceName,
                                             ReadingProgressJpaEntity progress) {
        var passage = passageRepository.findByLocationId(progress.getLocationId()).orElseThrow();
        var book = bookReadModel.findBook(space.id()).orElseThrow();
        return new RecentReadingSnapshot(space, spaceName, new BookSnapshot(book.bookId(), book.contentId(),
                book.title(), book.authors(), book.coverImageKey(), book.passageCount(), book.status(), book.available()),
                new ContentLocationId(progress.getLocationId()), passage.getSequence().value(),
                progress.getLastReadAt());
    }

}
