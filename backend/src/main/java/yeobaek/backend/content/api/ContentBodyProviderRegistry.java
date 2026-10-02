package yeobaek.backend.content.api;

@FunctionalInterface
public interface ContentBodyProviderRegistry {

    ContentBodyProvider get(String kind);
}
