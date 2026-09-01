package yeobaek.backend.support.analytics;

import com.posthog.server.PostHogInterface;

final class PostHogShutdown implements AutoCloseable {

    private final PostHogInterface postHog;

    PostHogShutdown(PostHogInterface postHog) {
        this.postHog = postHog;
    }

    @Override
    public void close() {
        try {
            postHog.flush();
        } finally {
            postHog.close();
        }
    }
}
