package com.yeobaek

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.yeobaek.core.analytics.AnalyticsClient
import com.yeobaek.core.app.AppContainer

@Suppress("FunctionName")
fun MainViewController(
    isDebug: Boolean,
    analyticsClient: AnalyticsClient,
) = ComposeUIViewController {
    val appContainer = remember {
        AppContainer(
            isDebug = isDebug,
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
