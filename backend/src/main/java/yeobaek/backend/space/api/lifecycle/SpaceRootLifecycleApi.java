package yeobaek.backend.space.api.lifecycle;

import yeobaek.backend.shared.identity.SpaceId;
import yeobaek.backend.space.api.SpaceKind;

public interface SpaceRootLifecycleApi {

    SpaceId create(SpaceKind kind);
}
