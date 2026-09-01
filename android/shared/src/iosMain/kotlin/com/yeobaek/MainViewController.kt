package com.yeobaek

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.yeobaek.core.analytics.AnalyticsClient
import com.yeobaek.core.app.AppContainer

@Suppress("FunctionName")
fun MainViewController(
    isDebug: Boolean,
    appVersion: String,
    analyticsClient: AnalyticsClient,
) = ComposeUIViewController {
    val appContainer = remember {
        AppContainer(
            isDebug = isDebug,
            appVersion = appVersion,
            analyticsClient = analyticsClient,
        )
    }

    DisposableEffect(appContainer) {
        onDispose {
            appContainer.close()
        }
    }

    App(
        appContainer = appContainer,
    )
}
