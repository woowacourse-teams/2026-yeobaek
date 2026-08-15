package com.yeobaek

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.yeobaek.core.app.AppContainer

@Suppress("FunctionName")
fun MainViewController() = ComposeUIViewController {
    val appContainer = remember {
        AppContainer(
            memberId = 7,
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
