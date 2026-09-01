package yeobaek.backend.support.analytics;

import com.posthog.server.PostHogInterface;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class AnalyticsConfiguration {

    @Bean
    public AnalyticsTracker analyticsTracker(ObjectProvider<PostHogInterface> postHogProvider,
                                             Environment environment) {
        PostHogInterface postHog = postHogProvider.getIfAvailable();
        if (postHog == null) {
            return NoOpAnalyticsTracker.INSTANCE;
        }
        return new PostHogAnalyticsTracker(postHog, activeProfiles(environment));
    }

    private static String activeProfiles(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return String.join(",", environment.getDefaultProfiles());
        }
        return String.join(",", activeProfiles);
    }
}
