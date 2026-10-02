package yeobaek.backend.bootstrap;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.content.api.ContentProvider;
import yeobaek.backend.content.api.ContentProviderRegistry;
import yeobaek.backend.content.api.ContentBodyProvider;
import yeobaek.backend.content.api.ContentBodyProviderRegistry;

@Configuration
public class ContentCapabilityConfiguration {

    @Bean
    ContentProviderRegistry contentProviders(List<ContentProvider> providers) {
        CapabilityRegistry<String, ContentProvider> registry =
                new CapabilityRegistry<>(providers, ContentProvider::supportedKind);
        return registry::get;
    }

    @Bean
    ContentBodyProviderRegistry contentBodyProviders(List<ContentBodyProvider> providers) {
        CapabilityRegistry<String, ContentBodyProvider> registry =
                new CapabilityRegistry<>(providers, ContentBodyProvider::supportedKind);
        return registry::get;
    }
}
