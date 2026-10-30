package com.yeobaek.core.analytics

class AnalyticsTracker(
    private val client: AnalyticsClient,
    commonProperties: Map<String, Any> = emptyMap(),
) {
    private val commonProperties = commonProperties.toMap()

    fun registerCommonProperties() {
        client.register(commonProperties)
    }

    fun resetIdentity() {
        client.reset()
        registerCommonProperties()
    }

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
