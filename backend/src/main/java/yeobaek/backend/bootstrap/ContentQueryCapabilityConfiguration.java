package yeobaek.backend.bootstrap;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.internal.legacyreference.ContentLegacyReferenceProvider;
import yeobaek.backend.content.internal.location.ContentLocationProvider;
import yeobaek.backend.content.internal.metadata.ContentMetadataProvider;

@Configuration
public class ContentQueryCapabilityConfiguration {

    @Bean
    Map<ContentKind, ContentMetadataProvider> contentMetadataProviders(List<ContentMetadataProvider> providers) {
        return CapabilityMap.from(providers, ContentMetadataProvider::supportedKind);
    }

    @Bean
    Map<ContentKind, ContentLegacyReferenceProvider> contentLegacyReferenceProviders(
            List<ContentLegacyReferenceProvider> providers
    ) {
        return CapabilityMap.from(providers, ContentLegacyReferenceProvider::supportedKind);
    }

    @Bean
    Map<ContentKind, ContentLocationProvider> contentLocationProviders(List<ContentLocationProvider> providers) {
        return CapabilityMap.from(providers, ContentLocationProvider::supportedKind);
    }
}
