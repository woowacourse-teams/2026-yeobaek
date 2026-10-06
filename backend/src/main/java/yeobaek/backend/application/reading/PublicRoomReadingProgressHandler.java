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
import yeobaek.backend.space.api.SpaceNotFoundException;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;

@Component
@RequiredArgsConstructor
public class PublicRoomReadingProgressHandler implements ReadingProgressHandler {

    private final PublicRoomApi rooms;
    private final ReadingProgressWorkflow workflow;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.PUBLIC_ROOM;
    }

    @Override
    public ReadingProgress update(MemberId actorId, SpaceId spaceId, ContentId contentId,
                                  ContentLocationId locationId, LocalDateTime readAt) {
        rooms.findBySpaceIdForUpdate(spaceId).orElseThrow(() -> new SpaceNotFoundException(spaceId,
                "독서 진도를 기록할 공개방 공간이 존재하지 않습니다: spaceId=" + spaceId.value()));
        return workflow.update(actorId, spaceId, contentId, locationId, readAt);
    }
}
