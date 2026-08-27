package com.yeobaek

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.yeobaek.core.app.AppContainer

@Suppress("FunctionName")
fun MainViewController(isDebug: Boolean) = ComposeUIViewController {
    val appContainer = remember {
        AppContainer(
            isDebug = isDebug,
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
