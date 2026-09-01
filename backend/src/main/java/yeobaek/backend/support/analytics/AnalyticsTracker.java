package yeobaek.backend.support.analytics;

@FunctionalInterface
public interface AnalyticsTracker {

    void track(Long memberId, AnalyticsEvent event);
}
