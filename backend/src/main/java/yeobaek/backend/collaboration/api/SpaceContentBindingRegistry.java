package yeobaek.backend.collaboration.api;

@FunctionalInterface
public interface SpaceContentBindingRegistry {

    SpaceContentBindingCapability get(String kind);
}
