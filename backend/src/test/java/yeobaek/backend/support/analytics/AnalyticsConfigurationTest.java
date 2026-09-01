package yeobaek.backend.support.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.posthog.server.PostHogInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

class AnalyticsConfigurationTest {

    private final AnalyticsConfiguration configuration = new AnalyticsConfiguration();

    @Test
    @DisplayName("PostHog가 비활성화되면 이벤트를 버리는 추적기를 생성한다")
    void createNoOpTrackerWithoutPostHog() {
        ObjectProvider<PostHogInterface> provider = mock();
        given(provider.getIfAvailable()).willReturn(null);

        AnalyticsTracker tracker = configuration.analyticsTracker(provider, new MockEnvironment());

        assertThat(tracker).isSameAs(NoOpAnalyticsTracker.INSTANCE);
    }

    @Test
    @DisplayName("PostHog가 활성화되면 활성 프로파일을 사용하는 추적기를 생성한다")
    void createPostHogTrackerWithActiveProfile() {
        ObjectProvider<PostHogInterface> provider = mock();
        given(provider.getIfAvailable()).willReturn(mock(PostHogInterface.class));
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        AnalyticsTracker tracker = configuration.analyticsTracker(provider, environment);

        assertThat(tracker).isInstanceOf(PostHogAnalyticsTracker.class);
    }
}
