package yeobaek.backend.content.spi;

import yeobaek.backend.content.api.ContentKind;

@FunctionalInterface
public interface ContentProviderRegistry {

    ContentProvider get(ContentKind kind);
}
