package yeobaek.backend.reading.api;

import java.time.LocalDateTime;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;

public record ReadingProgress(MemberId actorId, SpaceId spaceId, ContentId contentId,
                              ContentLocationId locationId, LocalDateTime lastReadAt) {

    public ReadingProgress {
        if (actorId == null || spaceId == null || contentId == null || locationId == null || lastReadAt == null) {
            throw new IllegalArgumentException("독서 진도의 필수 정보가 누락되었습니다.");
        }
    }
}
