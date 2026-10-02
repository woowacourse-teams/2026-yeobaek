package com.yeobaek.core.analytics

class AnalyticsTracker(
    private val client: AnalyticsClient,
) {
    fun track(event: AnalyticsEvent, additionalProperties: Map<String, Any> = emptyMap()) {
        client.capture(
            eventName = event.name,
            properties = event.properties + additionalProperties,
        )
    }

    fun trackScreen(screenName: String) {
        client.screen(screenName)
    }

    fun identify(userId: Int) {
        client.identify(userId.toString())
    }
}
