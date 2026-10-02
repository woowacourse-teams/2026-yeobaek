package yeobaek.backend.readmodel.reading;

import java.time.LocalDateTime;
import java.util.List;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.space.domain.Space;

@FunctionalInterface
public interface RecentReadingReadModel {

    List<RecentReadingSnapshot> findCandidates(MemberId actorId);

    record RecentReadingSnapshot(Space space, String spaceName, BookSnapshot book,
                                 ContentLocationId locationId, int passageSequence,
                                 LocalDateTime lastReadAt) {
    }

    record BookSnapshot(Long bookId, ContentId contentId, String title, List<String> authors,
                        String coverImageKey, int passageCount, String status, boolean available) {

        public BookSnapshot {
            authors = List.copyOf(authors);
        }
    }
}
