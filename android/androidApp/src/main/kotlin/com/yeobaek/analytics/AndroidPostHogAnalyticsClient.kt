package com.yeobaek.analytics

import android.app.Application
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.yeobaek.core.analytics.AnalyticsClient

class AndroidPostHogAnalyticsClient(
    application: Application,
) : AnalyticsClient {
    private val application = application
    private var isEnabled = false

    override fun setup(
        apiKey: String,
        host: String,
        isDebug: Boolean,
    ) {
        isEnabled = apiKey.isNotBlank()
        if (isEnabled) {
            val config = PostHogAndroidConfig(
                apiKey = apiKey,
                host = host,
            ).apply {
                debug = isDebug
                captureApplicationLifecycleEvents = false
                captureScreenViews = false
            }

            PostHogAndroid.setup(this.application, config)
        }
    }

    override fun capture(
        eventName: String,
        properties: Map<String, String>,
    ) {
        if (!isEnabled) return

        PostHog.capture(
            event = eventName,
            properties = properties,
        )
    }

    override fun screen(screenName: String) {
        if (!isEnabled) return

        PostHog.screen(screenTitle = screenName)
    }

    override fun identify(userId: String) {
        if (!isEnabled) return

        PostHog.identify(distinctId = userId)
    }
}
