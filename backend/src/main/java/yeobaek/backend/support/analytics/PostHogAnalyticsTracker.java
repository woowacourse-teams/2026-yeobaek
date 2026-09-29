package yeobaek.backend.support.analytics;

import static yeobaek.backend.support.LogField.MEMBER_ID;
import static yeobaek.backend.support.LogField.OPERATION;
import static yeobaek.backend.support.LogField.RESULT;
import static yeobaek.backend.support.LogField.RECOVERED;
import static yeobaek.backend.support.LogField.SUCCESS;

import com.posthog.server.PostHogCaptureOptions;
import com.posthog.server.PostHogInterface;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PostHogAnalyticsTracker implements AnalyticsTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostHogAnalyticsTracker.class);
    private static final int EVENT_SCHEMA_VERSION = 2;

    private final PostHogInterface postHog;
    private final String environment;

    PostHogAnalyticsTracker(PostHogInterface postHog, String environment) {
        this.postHog = postHog;
        this.environment = environment;
    }

    @Override
    public void track(Long memberId, AnalyticsEvent event) {
        LOGGER.atInfo()
                .addKeyValue(OPERATION, "analytics.track")
                .addKeyValue(MEMBER_ID, memberId)
                .addKeyValue("eventName", event.name())
                .log("분석 이벤트 전송을 시작합니다.");
        try {
            postHog.capture(memberId.toString(), event.name(), captureOptions(event));
            LOGGER.atInfo()
                    .addKeyValue(OPERATION, "analytics.track")
                    .addKeyValue(RESULT, SUCCESS)
                    .addKeyValue(MEMBER_ID, memberId)
                    .addKeyValue("eventName", event.name())
                    .log("분석 이벤트 전송 요청에 성공했습니다.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            LOGGER.atWarn()
                    .addKeyValue(OPERATION, "analytics.track")
                    .addKeyValue(RESULT, RECOVERED)
                    .addKeyValue(MEMBER_ID, memberId)
                    .addKeyValue("eventName", event.name())
                    .addKeyValue("exceptionType", exception.getClass().getSimpleName())
                    .log("분석 이벤트 전송 실패를 복구하고 요청 처리를 계속합니다.");
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
