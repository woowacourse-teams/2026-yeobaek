package com.yeobaek

import androidx.compose.ui.window.ComposeUIViewController
import com.yeobaek.core.app.AppContainer

@Suppress("FunctionName")
fun MainViewController(
    appContainer: AppContainer,
) = ComposeUIViewController {
    App(
        appContainer = appContainer,
    )
}
