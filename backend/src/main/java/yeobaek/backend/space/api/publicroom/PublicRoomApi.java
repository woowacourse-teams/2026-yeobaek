package yeobaek.backend.space.api.publicroom;

import java.util.List;
import java.util.Optional;
import yeobaek.backend.shared.identity.SpaceId;

public interface PublicRoomApi {

    PublicRoomResponse create();

    List<PublicRoomResponse> findAll();

    Optional<PublicRoomResponse> findById(Long publicRoomId);

    Optional<PublicRoomResponse> findByIdForUpdate(Long publicRoomId);

    Optional<PublicRoomResponse> findBySpaceId(SpaceId spaceId);

    Optional<PublicRoomResponse> findBySpaceIdForUpdate(SpaceId spaceId);
}
