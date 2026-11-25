package yeobaek.backend.support.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.posthog.server.PostHogCaptureOptions;
import com.posthog.server.PostHogInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

class AnalyticsConfigurationTest {

    private final AnalyticsConfiguration configuration = new AnalyticsConfiguration();

    @Test
    @DisplayName("PostHog가 비활성화되어도 이벤트를 남기는 요청은 계속 처리한다")
    void acceptEventsWithoutPostHog() {
        ObjectProvider<PostHogInterface> provider = mock();
        given(provider.getIfAvailable()).willReturn(null);

        AnalyticsTracker tracker = configuration.analyticsTracker(provider, new MockEnvironment());

        assertThatCode(() -> tracker.track(1L, AnalyticsEvent.memberCreate())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PostHog가 활성화되면 이벤트에 활성 프로파일을 담는다")
    void createPostHogTrackerWithActiveProfile() {
        PostHogInterface postHog = mock(PostHogInterface.class);
        ObjectProvider<PostHogInterface> provider = mock();
        given(provider.getIfAvailable()).willReturn(postHog);
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        AnalyticsTracker tracker = configuration.analyticsTracker(provider, environment);

        tracker.track(1L, AnalyticsEvent.memberCreate());

        var options = ArgumentCaptor.forClass(PostHogCaptureOptions.class);
        verify(postHog).capture(eq("1"), eq("backend_member_create"), options.capture());
        assertThat(options.getValue().getProperties()).containsEntry("environment", "prod");
    }
}
