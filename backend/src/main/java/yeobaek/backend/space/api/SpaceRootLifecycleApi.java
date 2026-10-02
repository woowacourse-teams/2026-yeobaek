package yeobaek.backend.space.api;

import yeobaek.backend.foundation.identity.SpaceId;

@FunctionalInterface
public interface SpaceRootLifecycleApi {

    SpaceId create(String kind);
}
