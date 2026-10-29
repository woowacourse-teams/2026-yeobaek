package com.yeobaek

import androidx.compose.runtime.Composable
import com.yeobaek.core.app.AppContainer
import com.yeobaek.devtools.DevToolsScreen

@Composable
fun DevTools(appContainer: AppContainer, navigateBack: () -> Unit) {
    DevToolsScreen(
        appContainer = appContainer,
        appBuild = BuildConfig.VERSION_CODE.toString(),
        buildType = BuildConfig.BUILD_TYPE,
        navigateBack = navigateBack,
    )
}
