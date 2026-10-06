package yeobaek.backend.reading.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

public interface ReadingProgressApi {

    ReadingProgress update(MemberId actorId, SpaceId spaceId, ContentId contentId,
                           ContentLocationId locationId, LocalDateTime readAt);

    Optional<ReadingProgress> find(MemberId actorId, SpaceId spaceId, ContentId contentId);

    List<ReadingProgress> findByActor(MemberId actorId);

    List<ReadingProgress> findBySpaces(MemberId actorId, List<SpaceId> spaceIds);

    void erase(MemberId actorId);
}
