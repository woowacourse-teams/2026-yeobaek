package yeobaek.backend.content.spi.location;

import yeobaek.backend.content.api.ContentKind;

@FunctionalInterface
public interface ContentLocationProviderRegistry {

    ContentLocationProvider get(ContentKind kind);
}
