package yeobaek.backend.space.api;

@FunctionalInterface
public interface SpaceAccessRegistry {

    SpaceAccessCapability get(String kind);
}
