package yeobaek.backend.space.publicroom.internal;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;
import yeobaek.backend.space.api.lifecycle.SpaceRootLifecycleApi;
import yeobaek.backend.space.api.publicroom.PublicRoomApi;
import yeobaek.backend.space.api.publicroom.PublicRoomResponse;
import yeobaek.backend.space.publicroom.persistence.PublicRoom;
import yeobaek.backend.space.publicroom.repository.PublicRoomRepository;

@Service
@RequiredArgsConstructor
public class PublicRoomService implements PublicRoomApi {

    private final PublicRoomRepository rooms;
    private final SpaceRootLifecycleApi spaceRoots;

    @Override
    @Transactional
    public PublicRoomResponse create() {
        SpaceId spaceId = spaceRoots.create(SpaceKind.PUBLIC_ROOM);
        var room = rooms.save(PublicRoom.create(spaceId.value()));
        return toResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicRoomResponse> findAll() {
        return rooms.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublicRoomResponse> findById(Long publicRoomId) {
        return rooms.findById(publicRoomId).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublicRoomResponse> findByIdForUpdate(Long publicRoomId) {
        return rooms.findByIdForUpdate(publicRoomId).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublicRoomResponse> findBySpaceId(SpaceId spaceId) {
        return rooms.findBySpaceRootId(spaceId.value()).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublicRoomResponse> findBySpaceIdForUpdate(SpaceId spaceId) {
        return rooms.findBySpaceRootIdForUpdate(spaceId.value()).map(this::toResponse);
    }

    private PublicRoomResponse toResponse(PublicRoom room) {
        return new PublicRoomSnapshot(new SpaceId(room.getSpaceId()), room.getId());
    }
}
