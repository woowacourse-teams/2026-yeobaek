package yeobaek.backend.space.api;

import yeobaek.backend.shared.identity.SpaceId;

public interface Space {

    SpaceId id();

    SpaceKind kind();
}
