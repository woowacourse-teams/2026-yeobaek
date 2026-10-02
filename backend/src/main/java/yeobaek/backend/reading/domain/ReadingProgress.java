package yeobaek.backend.reading.domain;

import java.time.LocalDateTime;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;

public record ReadingProgress(MemberId actorId, SpaceId spaceId, ContentId contentId,
                              ContentLocationId locationId, LocalDateTime lastReadAt) {

    public ReadingProgress {
        if (actorId == null || spaceId == null || contentId == null || locationId == null || lastReadAt == null) {
            throw new IllegalArgumentException("독서 진도의 필수 정보가 누락되었습니다.");
        }
    }
}
