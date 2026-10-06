package yeobaek.backend.bootstrap;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.spi.ContentProvider;
import yeobaek.backend.content.spi.ContentProviderRegistry;
import yeobaek.backend.content.spi.body.ContentBodyProvider;
import yeobaek.backend.content.spi.body.ContentBodyProviderRegistry;

@Configuration
public class ContentCapabilityConfiguration {

    @Bean
    ContentProviderRegistry contentProviders(List<ContentProvider> providers) {
        CapabilityRegistry<ContentKind, ContentProvider> registry =
                new CapabilityRegistry<>(providers, ContentProvider::supportedKind);
        return registry::get;
    }

    @Bean
    ContentBodyProviderRegistry contentBodyProviders(List<ContentBodyProvider> providers) {
        CapabilityRegistry<ContentKind, ContentBodyProvider> registry =
                new CapabilityRegistry<>(providers, ContentBodyProvider::supportedKind);
        return registry::get;
    }
}
