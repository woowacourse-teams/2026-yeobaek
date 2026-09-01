package yeobaek.backend.support.analytics;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "posthog")
public record PostHogProperties(
        String apiKey,
        URI host
) {

    public PostHogProperties {
        requireNonBlank(apiKey, "posthog.api-key");
        requireHttpsUrl(host);
    }

    private static void requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(propertyName + " 설정은 PostHog를 활성화할 때 필수입니다.");
        }
    }

    private static void requireHttpsUrl(URI value) {
        if (value == null || !value.isAbsolute() || value.getHost() == null
                || !"https".equalsIgnoreCase(value.getScheme())) {
            throw new IllegalArgumentException("posthog.host는 절대 HTTPS URL이어야 합니다.");
        }
    }
}
