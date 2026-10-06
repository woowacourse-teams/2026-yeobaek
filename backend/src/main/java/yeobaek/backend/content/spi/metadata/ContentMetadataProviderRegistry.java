package yeobaek.backend.content.spi.metadata;

import yeobaek.backend.content.api.ContentKind;

@FunctionalInterface
public interface ContentMetadataProviderRegistry {

    ContentMetadataProvider get(ContentKind kind);
}
