package yeobaek.backend.publicroom.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.foundation.identity.SpaceId;
import yeobaek.backend.publicroom.api.PublicRoomProvisioningApi;
import yeobaek.backend.publicroom.repository.PublicRoomRepository;
import yeobaek.backend.space.domain.PublicRoom;
import yeobaek.backend.space.api.SpaceRootLifecycleApi;

@Service
@RequiredArgsConstructor
public class PublicRoomProvisioningService implements PublicRoomProvisioningApi {

    private final PublicRoomRepository publicRoomRepository;
    private final SpaceRootLifecycleApi spaceRoots;

    @Override
    @Transactional
    public PublicRoom create() {
        SpaceId spaceId = spaceRoots.create("PUBLIC_ROOM");
        var room = publicRoomRepository.save(yeobaek.backend.publicroom.domain.PublicRoom.create(spaceId.value()));
        return new PublicRoom(spaceId, room.getId());
    }
}
