package yeobaek.backend.reading.api;

import java.time.LocalDateTime;
import java.util.Optional;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.reading.domain.ReadingProgress;

public interface ReadingProgressApi {

    ReadingProgress update(MemberId actorId, SpaceId spaceId, ContentId contentId,
                           ContentLocationId locationId, LocalDateTime readAt);

    Optional<ReadingProgress> find(MemberId actorId, SpaceId spaceId, ContentId contentId);

    void erase(MemberId actorId);
}
