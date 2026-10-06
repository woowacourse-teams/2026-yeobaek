package yeobaek.backend.space.publicroom.internal;

import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.publicroom.PublicRoomResponse;
import yeobaek.backend.space.api.SpaceKind;

record PublicRoomSnapshot(SpaceId id, Long publicRoomId) implements PublicRoomResponse {

    PublicRoomSnapshot {
        if (id == null || publicRoomId == null) {
            throw new IllegalArgumentException("공개방의 필수 정보가 누락되었습니다.");
        }
    }

    @Override
    public SpaceKind kind() {
        return SpaceKind.PUBLIC_ROOM;
    }
}
