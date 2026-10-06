package yeobaek.backend.collaboration.spi.binding;

import yeobaek.backend.space.api.SpaceKind;

@FunctionalInterface
public interface SpaceContentBindingRegistry {

    SpaceContentBindingCapability get(SpaceKind kind);
}
