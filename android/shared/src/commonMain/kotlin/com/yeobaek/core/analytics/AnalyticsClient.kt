package com.yeobaek.core.analytics

interface AnalyticsClient {
    fun setup(
        apiKey: String,
        host: String,
        isDebug: Boolean,
    )

    fun register(properties: Map<String, Any>)

    fun capture(
        eventName: String,
        properties: Map<String, Any>,
    )

    fun screen(screenName: String, properties: Map<String, Any>)

    fun identify(userId: String)

    fun reset()
}

object NoOpAnalyticsClient : AnalyticsClient {
    override fun setup(
        apiKey: String,
        host: String,
        isDebug: Boolean,
    ) = Unit

    override fun register(properties: Map<String, Any>) = Unit

    override fun capture(
        eventName: String,
        properties: Map<String, Any>,
    ) = Unit

    override fun screen(screenName: String, properties: Map<String, Any>) = Unit

    override fun identify(userId: String) = Unit

    override fun reset() = Unit
}
