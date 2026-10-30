package com.yeobaek

import androidx.compose.ui.window.ComposeUIViewController
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.update.IOSRequiredUpdateGate

@Suppress("FunctionName")
fun MainViewController(
    appContainer: AppContainer,
) = ComposeUIViewController {
    IOSRequiredUpdateGate(appContainer) {
        App(appContainer = appContainer)
    }
}
