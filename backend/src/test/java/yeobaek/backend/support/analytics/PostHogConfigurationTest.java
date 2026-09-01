package yeobaek.backend.support.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.posthog.server.PostHogInterface;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PostHogConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PostHogConfiguration.class);

    @Test
    @DisplayName("PostHog는 기본적으로 비활성화된다")
    void disableByDefault() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(PostHogInterface.class));
    }

    @Test
    @DisplayName("PostHog를 활성화하면 SDK 빈을 생성한다")
    void createPostHogBeanWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "posthog.enabled=true",
                        "posthog.api-key=phc_test",
                        "posthog.host=https://us.i.posthog.com"
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(PostHogInterface.class));
    }

    @Test
    @DisplayName("API 키 없이 PostHog를 활성화하면 애플리케이션 시작에 실패한다")
    void rejectMissingApiKeyWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "posthog.enabled=true",
                        "posthog.api-key=",
                        "posthog.host=https://us.i.posthog.com"
                )
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(IllegalArgumentException.class)
                        .hasStackTraceContaining("posthog.api-key"));
    }
}
