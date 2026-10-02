package yeobaek.backend.space.domain;

import yeobaek.backend.foundation.identity.SpaceId;

public record Club(SpaceId id, Long clubId, String name, String joinCode) implements Space {

    public static final String KIND = "CLUB";

    public Club {
        if (id == null || clubId == null || name == null || joinCode == null) {
            throw new IllegalArgumentException("독서 모임의 필수 정보가 누락되었습니다.");
        }
    }

    @Override
    public String kind() {
        return KIND;
    }
}
