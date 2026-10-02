package yeobaek.backend.collaboration.api;

import yeobaek.backend.foundation.identity.SpaceId;

public class ContentBindingNotFoundFailure extends RuntimeException {

    public ContentBindingNotFoundFailure(SpaceId spaceId) {
        super("공간에 연결된 콘텐츠가 없습니다: spaceId=" + spaceId.value());
    }
}
