package yeobaek.backend.publicroom.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.foundation.identity.MemberId;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.space.api.SpaceAccessCapability;
import yeobaek.backend.space.domain.PublicRoom;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;

@Component
@RequiredArgsConstructor
public class PublicRoomSpaceAccessCapability implements SpaceAccessCapability {

    private final PublicRoomRepository publicRoomRepository;

    @Override
    public String supportedKind() {
        return PublicRoom.KIND;
    }

    @Override
    public yeobaek.backend.space.domain.Space getSpace(SpaceId spaceId) {
        var room = publicRoomRepository.findBySpaceRootId(spaceId.value()).orElseThrow();
        return new PublicRoom(spaceId, room.getId());
    }

    @Override
    public boolean canAccess(MemberId memberId, SpaceId spaceId) {
        return true;
    }
}
