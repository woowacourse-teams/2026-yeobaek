package yeobaek.backend.support.analytics;

import com.posthog.server.PostHogCaptureOptions;
import com.posthog.server.PostHogInterface;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PostHogAnalyticsTracker implements AnalyticsTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostHogAnalyticsTracker.class);
    private static final int EVENT_SCHEMA_VERSION = 1;

    private final PostHogInterface postHog;
    private final String environment;

    PostHogAnalyticsTracker(PostHogInterface postHog, String environment) {
        this.postHog = postHog;
        this.environment = environment;
    }

    @Override
    public void track(Long memberId, AnalyticsEvent event) {
        try {
            postHog.capture(memberId.toString(), event.name(), captureOptions(event));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("PostHog 이벤트 전송 요청에 실패했습니다. event={}, cause={}",
                        event.name(), exception.getClass().getSimpleName());
            }
        }
    }

    private PostHogCaptureOptions captureOptions(AnalyticsEvent event) {
        Map<String, Object> properties = new LinkedHashMap<>(event.properties());
        properties.put("source", "backend");
        properties.put("environment", environment);
        properties.put("event_schema_version", EVENT_SCHEMA_VERSION);
        properties.put("$process_person_profile", false);
        return PostHogCaptureOptions.builder()
                .properties(properties)
                .build();
    }
}
