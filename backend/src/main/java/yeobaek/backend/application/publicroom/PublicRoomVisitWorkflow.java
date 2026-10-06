package yeobaek.backend.application.publicroom;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.collaboration.api.binding.SpaceContentBindingApi;
import yeobaek.backend.content.api.ContentApi;
import yeobaek.backend.shared.identity.ContentId;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceNotFoundException;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;
import yeobaek.backend.space.api.publicroom.PublicRoomVisitApi;

@Service
@RequiredArgsConstructor
public class PublicRoomVisitWorkflow {

    private final PublicRoomApi rooms;
    private final PublicRoomVisitApi visits;
    private final SpaceContentBindingApi bindings;
    private final ContentApi contents;
    private final Clock clock = Clock.systemDefaultZone();

    @Transactional
    public void visit(MemberId actorId, SpaceId spaceId) {
        rooms.findBySpaceIdForUpdate(spaceId).orElseThrow(() -> new SpaceNotFoundException(spaceId,
                "방문할 공개방 공간이 존재하지 않습니다: spaceId=" + spaceId.value()));
        ContentId contentId = bindings.findContents(spaceId).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("공개방에 연결된 컨텐츠가 없습니다."));
        contents.requireAvailable(contentId);
        visits.visit(actorId, spaceId, LocalDateTime.now(clock));
    }
}
