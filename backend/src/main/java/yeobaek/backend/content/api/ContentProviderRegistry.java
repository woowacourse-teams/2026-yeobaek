package yeobaek.backend.content.api;

@FunctionalInterface
public interface ContentProviderRegistry {

    ContentProvider get(String kind);
}
