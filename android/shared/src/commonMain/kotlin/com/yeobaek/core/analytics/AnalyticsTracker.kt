package com.yeobaek.core.analytics

class AnalyticsTracker(
    private val client: AnalyticsClient,
) {
    fun track(event: AnalyticsEvent) {
        client.capture(
            eventName = event.name,
            properties = event.properties,
        )
    }

    fun trackScreen(screenName: String) {
        client.screen(screenName)
    }

    fun identify(userId: Long) {
        client.identify(userId.toString())
    }
}
