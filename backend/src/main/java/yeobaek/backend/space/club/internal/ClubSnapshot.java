package yeobaek.backend.space.club.internal;

import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.club.ClubResponse;
import yeobaek.backend.space.api.SpaceKind;

record ClubSnapshot(SpaceId id, Long clubId, String name, String joinCode) implements ClubResponse {

    ClubSnapshot {
        if (id == null || clubId == null || name == null || joinCode == null) {
            throw new IllegalArgumentException("독서 모임의 필수 정보가 누락되었습니다.");
        }
    }

    @Override
    public SpaceKind kind() {
        return SpaceKind.CLUB;
    }
}
