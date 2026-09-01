package com.yeobaek.core.analytics

interface AnalyticsClient {
    fun setup(
        apiKey: String,
        host: String,
        isDebug: Boolean,
    )

    fun capture(
        eventName: String,
        properties: Map<String, String>,
    )

    fun screen(screenName: String)

    fun identify(userId: String)
}

object NoOpAnalyticsClient : AnalyticsClient {
    override fun setup(
        apiKey: String,
        host: String,
        isDebug: Boolean,
    ) = Unit

    override fun capture(
        eventName: String,
        properties: Map<String, String>,
    ) = Unit

    override fun screen(screenName: String) = Unit

    override fun identify(userId: String) = Unit
}
