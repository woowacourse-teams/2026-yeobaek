package yeobaek.backend.space.publicroom.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import yeobaek.backend.shared.identity.MemberId;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.spi.access.SpaceAccessCapability;
import yeobaek.backend.space.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.space.api.SpaceKind;

@Component
@RequiredArgsConstructor
public class PublicRoomSpaceAccessCapability implements SpaceAccessCapability {

    private final PublicRoomRepository publicRoomRepository;

    @Override
    public SpaceKind supportedKind() {
        return SpaceKind.PUBLIC_ROOM;
    }

    @Override
    public yeobaek.backend.space.api.Space getSpace(SpaceId spaceId) {
        var room = publicRoomRepository.findBySpaceRootId(spaceId.value()).orElseThrow();
        return new PublicRoomSnapshot(spaceId, room.getId());
    }

    @Override
    public boolean canAccess(MemberId memberId, SpaceId spaceId) {
        return true;
    }
}
