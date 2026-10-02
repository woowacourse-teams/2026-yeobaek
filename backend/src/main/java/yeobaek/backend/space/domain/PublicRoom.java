package yeobaek.backend.space.domain;

import yeobaek.backend.foundation.identity.SpaceId;

public record PublicRoom(SpaceId id, Long publicRoomId) implements Space {

    public static final String KIND = "PUBLIC_ROOM";

    public PublicRoom {
        if (id == null || publicRoomId == null) {
            throw new IllegalArgumentException("공개방의 필수 정보가 누락되었습니다.");
        }
    }

    @Override
    public String kind() {
        return KIND;
    }
}
