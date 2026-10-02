package yeobaek.backend.publicroom.api;

import yeobaek.backend.space.domain.PublicRoom;

@FunctionalInterface
public interface PublicRoomProvisioningApi {

    PublicRoom create();
}
