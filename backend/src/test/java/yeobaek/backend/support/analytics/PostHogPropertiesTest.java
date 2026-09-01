package yeobaek.backend.support.analytics;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostHogPropertiesTest {

    @Test
    @DisplayName("API 키는 공백일 수 없다")
    void rejectBlankApiKey() {
        assertThatThrownBy(() -> new PostHogProperties(" ", URI.create("https://us.i.posthog.com")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("수집 호스트는 절대 HTTPS URL이어야 한다")
    void rejectInvalidHost() {
        assertThatThrownBy(() -> new PostHogProperties("phc_test", URI.create("http://us.i.posthog.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PostHogProperties("phc_test", URI.create("/capture")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
