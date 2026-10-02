package yeobaek.backend.application.reading;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.collaboration.api.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.foundation.identity.ContentId;
import yeobaek.backend.foundation.identity.ContentLocationId;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.reading.domain.ReadingProgress;
import yeobaek.backend.space.api.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class ReadingProgressWorkflow {

    private final MemberQuery memberQuery;
    private final SpaceAccessApi spaceAccessApi;
    private final SpaceContentBindingApi bindingApi;
    private final ContentApi contentApi;
    private final ReadingProgressApi progressApi;

    @Transactional
    public ReadingProgress update(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                  ContentLocationId locationId, LocalDateTime readAt) {
        memberQuery.getProfile(actorId);
        if (!spaceAccessApi.canAccess(actorId, spaceId)) {
            throw new ReadingProgressPolicyFailure(ReadingProgressPolicyFailure.Reason.SPACE_ACCESS_DENIED,
                    "진도를 기록할 공간에 접근할 수 없습니다.");
        }
        if (!bindingApi.isBound(spaceId, contentId)) {
            throw new ReadingProgressPolicyFailure(ReadingProgressPolicyFailure.Reason.CONTENT_NOT_BOUND,
                    "공간에 연결되지 않은 컨텐츠입니다.");
        }
        if (!contentApi.ownsLocation(contentId, locationId)) {
            throw new ReadingProgressPolicyFailure(ReadingProgressPolicyFailure.Reason.LOCATION_NOT_IN_CONTENT,
                    "컨텐츠에 속하지 않는 읽기 위치입니다.");
        }
        contentApi.requireAvailable(contentId);
        return progressApi.update(actorId, spaceId, contentId, locationId, readAt);
    }
}
