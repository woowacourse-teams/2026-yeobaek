package yeobaek.backend.space.domain;

import yeobaek.backend.foundation.identity.SpaceId;

public interface Space {

    SpaceId id();

    String kind();
}
