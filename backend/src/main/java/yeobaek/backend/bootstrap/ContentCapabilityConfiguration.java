package yeobaek.backend.bootstrap;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import yeobaek.backend.content.api.ContentKind;
import yeobaek.backend.content.internal.ContentProvider;
import yeobaek.backend.content.internal.body.ContentBodyProvider;

@Configuration
public class ContentCapabilityConfiguration {

    @Bean
    Map<ContentKind, ContentProvider> contentProviders(List<ContentProvider> providers) {
        return CapabilityMap.from(providers, ContentProvider::supportedKind);
    }

    @Bean
    Map<ContentKind, ContentBodyProvider> contentBodyProviders(List<ContentBodyProvider> providers) {
        return CapabilityMap.from(providers, ContentBodyProvider::supportedKind);
    }
}
