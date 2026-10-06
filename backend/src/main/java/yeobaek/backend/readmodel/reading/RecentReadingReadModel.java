package yeobaek.backend.readmodel.reading;

import java.time.LocalDateTime;
import java.util.List;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.space.api.Space;

public interface RecentReadingReadModel {

    List<RecentReadingSnapshot> findCandidates(MemberId actorId);

    record RecentReadingSnapshot(Space space, String spaceName, ContentSnapshot content,
                                 ContentLocationId locationId, int passageSequence,
                                 LocalDateTime lastReadAt) {
    }

    record ContentSnapshot(ContentId contentId, ContentKind kind, String title, List<String> creators,
                           String coverImageUrl, int unitCount, boolean available) {

        public ContentSnapshot {
            creators = List.copyOf(creators);
        }
    }
}
