package yeobaek.backend.application.reading;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.content.api.location.ContentLocationQueryApi;
import yeobaek.backend.content.api.location.LocationKind;
import yeobaek.backend.shared.exception.ErrorCode;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.ContentLocationId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.member.api.MemberQuery;
import yeobaek.backend.reading.api.ReadingProgressApi;
import yeobaek.backend.reading.api.ReadingProgress;
import yeobaek.backend.space.api.access.SpaceAccessApi;

@Service
@RequiredArgsConstructor
public class ReadingProgressWorkflow {

    private final MemberQuery memberQuery;
    private final SpaceAccessApi spaceAccessApi;
    private final SpaceContentBindingApi bindingApi;
    private final ContentApi contentApi;
    private final ReadingProgressApi progressApi;
    private final ContentLocationQueryApi locations;

    @Transactional
    public ReadingProgress update(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                  ContentLocationId locationId, LocalDateTime readAt) {
        memberQuery.getProfile(actorId);
        if (!spaceAccessApi.canAccess(actorId, spaceId)) {
            throw new ReadingProgressPolicyException(ErrorCode.SPACE_ACCESS_DENIED,
                    "진도를 기록할 공간에 접근할 수 없습니다: spaceId=" + spaceId.value(),
                    Map.of("actorId", Long.toString(actorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "locationId", Long.toString(locationId.value()),
                            "reason", "SPACE_ACCESS_DENIED"));
        }
        if (!bindingApi.isBound(spaceId, contentId)) {
            throw new ReadingProgressPolicyException(ErrorCode.CONTENT_NOT_BOUND,
                    "진도를 기록할 공간에 연결되지 않은 컨텐츠입니다: spaceId=" + spaceId.value()
                            + ", contentId=" + contentId.value(),
                    Map.of("actorId", Long.toString(actorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "locationId", Long.toString(locationId.value()),
                            "reason", "CONTENT_NOT_BOUND"));
        }
        var location = locations.get(locationId);
        if (!location.contentId().equals(contentId) || !LocationKind.PASSAGE.equals(location.kind())) {
            throw new ReadingProgressPolicyException(ErrorCode.INVALID_REQUEST,
                    "진도를 기록할 위치가 컨텐츠의 문단이 아닙니다: contentId=" + contentId.value()
                            + ", locationId=" + locationId.value(),
                    Map.of("actorId", Long.toString(actorId.value()),
                            "spaceId", Long.toString(spaceId.value()),
                            "contentId", Long.toString(contentId.value()),
                            "locationId", Long.toString(locationId.value()),
                            "actualLocationKind", location.kind().value(),
                            "reason", "LOCATION_NOT_IN_CONTENT"));
        }
        contentApi.requireAvailable(contentId);
        return progressApi.update(actorId, spaceId, contentId, locationId, readAt);
    }
}
