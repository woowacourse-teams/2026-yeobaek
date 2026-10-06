package yeobaek.backend.content.spi.legacyreference;

import yeobaek.backend.content.api.ContentKind;

@FunctionalInterface
public interface ContentLegacyReferenceProviderRegistry {

    ContentLegacyReferenceProvider get(ContentKind kind);
}
