package yeobaek.backend.support.analytics;

enum NoOpAnalyticsTracker implements AnalyticsTracker {

    INSTANCE;

    @Override
    public void track(Long memberId, AnalyticsEvent event) {
        // PostHog가 비활성화된 환경에서는 이벤트를 전송하지 않는다.
    }
}
