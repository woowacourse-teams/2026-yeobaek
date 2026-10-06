package yeobaek.backend.application.reading;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.content.api.body.ContentBodyApi;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.access.SpaceAccessApi;
import yeobaek.backend.space.api.club.ProgressRate;

@Service
@RequiredArgsConstructor
public class ReadingProgressCommandService {

    private final SpaceAccessApi spaces;
    private final ReadingProgressHandlerRegistry handlers;
    private final ContentLocationQueryApi locations;
    private final ContentBodyApi bodies;
    private final Clock clock = Clock.systemDefaultZone();

    @Transactional
    public ProgressResult update(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                 ContentLocationId locationId) {
        LocalDateTime readAt = LocalDateTime.now(clock);
        var space = spaces.getSpace(spaceId);
        var changed = handlers.get(space.kind()).update(actorId, spaceId, contentId, locationId, readAt);
        int sequence = locations.get(changed.locationId()).passageSequence();
        int rate = ProgressRate.calculate(sequence, bodies.passageCount(contentId)).roundedPercentage();
        return new ProgressResult(sequence, rate, changed.lastReadAt());
    }

    public record ProgressResult(int lastReadPassageSequence, int progressRate, LocalDateTime lastReadAt) {
    }
}
