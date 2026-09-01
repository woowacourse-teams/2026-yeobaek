package com.yeobaek

import android.app.Application
import com.yeobaek.analytics.AndroidPostHogAnalyticsClient
import com.yeobaek.core.app.AppContainer

class YeobaekApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        val isDebug = BuildConfig.DEBUG
        appContainer = AppContainer(
            isDebug = isDebug,
            appVersion = BuildConfig.VERSION_NAME,
            analyticsClient = AndroidPostHogAnalyticsClient(
                application = this,
            ),
        )
    }
}
