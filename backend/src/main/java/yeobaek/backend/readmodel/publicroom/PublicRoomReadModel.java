package yeobaek.backend.readmodel.publicroom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.domain.PublicRoom;

public interface PublicRoomReadModel {

    List<RoomSnapshot> findAllActive();

    Optional<RoomSnapshot> findRoom(Long publicRoomId);

    Optional<RoomSnapshot> findRoomForUpdate(Long publicRoomId);

    List<VisitedRoomSnapshot> findVisitedActive(MemberId actorId);

    Map<SpaceId, ProgressSnapshot> findProgresses(MemberId actorId, List<SpaceId> spaceIds);

    Optional<ProgressSnapshot> findProgress(MemberId actorId, SpaceId spaceId, ContentId contentId);

    Optional<LocalDateTime> findLastVisit(MemberId actorId, SpaceId spaceId);

    Map<SpaceId, Long> countVisitors(List<SpaceId> spaceIds);

    List<PassageSnapshot> findPassages(Long bookId, int from, int to);

    Optional<PassageLocationSnapshot> findPassage(Long passageId);

    Map<Long, Long> countVisibleComments(MemberId actorId, Long publicRoomId, List<Long> sentenceIds);

    record RoomSnapshot(PublicRoom space, BookSnapshot book) {
    }

    record BookSnapshot(Long bookId, ContentId contentId, String title, List<String> authors,
                        String coverImageKey, int passageCount, String status, boolean available) {

        public BookSnapshot {
            authors = List.copyOf(authors);
        }
    }

    record ProgressSnapshot(ContentLocationId locationId, int passageSequence, LocalDateTime lastReadAt) {
    }

    record VisitedRoomSnapshot(RoomSnapshot room, ProgressSnapshot progress, LocalDateTime lastVisitedAt) {
    }

    record PassageSnapshot(Long passageId, int sequence, Long chapterId, List<SentenceSnapshot> sentences) {

        public PassageSnapshot {
            sentences = List.copyOf(sentences);
        }
    }

    record SentenceSnapshot(Long sentenceId, int sequence, String content) {
    }

    record PassageLocationSnapshot(Long passageId, Long bookId, ContentLocationId locationId, int sequence) {
    }
}
