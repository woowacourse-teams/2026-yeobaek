package yeobaek.backend.application.reading;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.reading.api.ReadingProgress;
import yeobaek.backend.space.api.SpaceKind;

@Component
@RequiredArgsConstructor
public class ClubReadingProgressHandler implements ReadingProgressHandler {

    private final ReadingProgressWorkflow workflow;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.CLUB;
    }

    @Override
    public ReadingProgress update(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                  ContentLocationId locationId, LocalDateTime readAt) {
        return workflow.update(actorId, spaceId, contentId, locationId, readAt);
    }
}
