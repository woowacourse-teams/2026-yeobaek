package yeobaek.backend.space.spi.access;

import yeobaek.backend.space.api.SpaceKind;

@FunctionalInterface
public interface SpaceAccessRegistry {

    SpaceAccessCapability get(SpaceKind kind);
}
