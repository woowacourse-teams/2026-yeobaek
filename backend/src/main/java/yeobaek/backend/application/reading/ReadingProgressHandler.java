package yeobaek.backend.application.reading;

import java.time.LocalDateTime;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.reading.api.ReadingProgress;
import yeobaek.backend.space.api.SpaceKind;

public interface ReadingProgressHandler {

    SpaceKind supportedKind();

    ReadingProgress update(MemberId actorId, SpaceId spaceId, ContentId contentId,
                           ContentLocationId locationId, LocalDateTime readAt);
}
