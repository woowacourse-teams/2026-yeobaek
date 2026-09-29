package yeobaek.backend.support.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum NoOpAnalyticsTracker implements AnalyticsTracker {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpAnalyticsTracker.class);

    @Override
    public void track(Long memberId, AnalyticsEvent event) {
        LOGGER.atInfo().addKeyValue("operation", "analytics.track").addKeyValue("phase", "attempt")
                .addKeyValue("enabled", false).addKeyValue("eventName", event.name())
                .log("비활성화된 분석 이벤트 처리를 시작합니다.");
        LOGGER.atInfo().addKeyValue("operation", "analytics.track").addKeyValue("phase", "success")
                .addKeyValue("enabled", false).addKeyValue("eventName", event.name())
                .log("분석 이벤트를 전송하지 않고 처리를 완료했습니다.");
    }
}
