package yeobaek.backend.content.spi.body;

import yeobaek.backend.content.api.ContentKind;

@FunctionalInterface
public interface ContentBodyProviderRegistry {

    ContentBodyProvider get(ContentKind kind);
}
