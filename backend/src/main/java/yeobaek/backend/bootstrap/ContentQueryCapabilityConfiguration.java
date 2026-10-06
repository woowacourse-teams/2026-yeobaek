package yeobaek.backend.bootstrap;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.spi.legacyreference.ContentLegacyReferenceProvider;
import yeobaek.backend.content.spi.legacyreference.ContentLegacyReferenceProviderRegistry;
import yeobaek.backend.content.spi.metadata.ContentMetadataProvider;
import yeobaek.backend.content.spi.metadata.ContentMetadataProviderRegistry;
import yeobaek.backend.content.spi.location.ContentLocationProvider;
import yeobaek.backend.content.spi.location.ContentLocationProviderRegistry;

@Configuration
public class ContentQueryCapabilityConfiguration {

    @Bean
    ContentMetadataProviderRegistry contentMetadataProviders(List<ContentMetadataProvider> providers) {
        CapabilityRegistry<ContentKind, ContentMetadataProvider> registry =
                new CapabilityRegistry<>(providers, ContentMetadataProvider::supportedKind);
        return registry::get;
    }

    @Bean
    ContentLegacyReferenceProviderRegistry contentLegacyReferenceProviders(
            List<ContentLegacyReferenceProvider> providers
    ) {
        CapabilityRegistry<ContentKind, ContentLegacyReferenceProvider> registry =
                new CapabilityRegistry<>(providers, ContentLegacyReferenceProvider::supportedKind);
        return registry::get;
    }

    @Bean
    ContentLocationProviderRegistry contentLocationProviders(List<ContentLocationProvider> providers) {
        CapabilityRegistry<ContentKind, ContentLocationProvider> registry =
                new CapabilityRegistry<>(providers, ContentLocationProvider::supportedKind);
        return registry::get;
    }
}
