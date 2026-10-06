package yeobaek.backend.support.analytics;

public interface AnalyticsTracker {

    void track(Long memberId, AnalyticsEvent event);
}
