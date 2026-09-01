package yeobaek.backend.support.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.posthog.server.PostHogCaptureOptions;
import com.posthog.server.PostHogInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PostHogAnalyticsTrackerTest {

    @Test
    @DisplayName("식별자와 공통 속성을 포함해 PostHog 이벤트를 전송한다")
    void captureEventWithCommonProperties() {
        PostHogInterface postHog = mock(PostHogInterface.class);
        var tracker = new PostHogAnalyticsTracker(postHog, "prod");
        var event = AnalyticsEvent.clubCreated(10L, 5L);

        tracker.track(1L, event);

        var optionsCaptor = ArgumentCaptor.forClass(PostHogCaptureOptions.class);
        verify(postHog).capture(eq("1"), eq("backend_club_created"), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue().getProperties())
                .containsEntry("club_id", 10L)
                .containsEntry("book_id", 5L)
                .containsEntry("source", "backend")
                .containsEntry("environment", "prod")
                .containsEntry("event_schema_version", 1)
                .containsEntry("$process_person_profile", false);
    }

    @Test
    @DisplayName("PostHog 전송 요청 실패를 API 흐름으로 전파하지 않는다")
    void isolatePostHogFailure() {
        PostHogInterface postHog = mock(PostHogInterface.class);
        var tracker = new PostHogAnalyticsTracker(postHog, "prod");
        doThrow(new IllegalStateException("capture failed"))
                .when(postHog).capture(eq("1"), eq("backend_member_created"), any());

        assertThatCode(() -> tracker.track(1L, AnalyticsEvent.memberCreated()))
                .doesNotThrowAnyException();
    }
}
