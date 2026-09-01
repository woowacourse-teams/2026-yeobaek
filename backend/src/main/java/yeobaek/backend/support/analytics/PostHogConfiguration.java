package yeobaek.backend.support.analytics;

import com.posthog.server.PostHog;
import com.posthog.server.PostHogConfig;
import com.posthog.server.PostHogInterface;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "posthog", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PostHogProperties.class)
public class PostHogConfiguration {

    @Bean(destroyMethod = "")
    public PostHogInterface postHog(PostHogProperties properties) {
        PostHogConfig config = PostHogConfig.builder(properties.apiKey())
                .host(properties.host().toString())
                .build();
        return PostHog.with(config);
    }

    @Bean
    PostHogShutdown postHogShutdown(PostHogInterface postHog) {
        return new PostHogShutdown(postHog);
    }
}
