package com.yeobaek

import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.core.update.IOSRequiredUpdateGate
import com.yeobaek.devtools.DevApp
import com.yeobaek.devtools.DevToolsScreen
import kotlinx.serialization.Serializable

@Suppress("FunctionName")
fun DebugViewController(
    appContainer: AppContainer,
    appBuild: String,
) = ComposeUIViewController {
    IOSRequiredUpdateGate(appContainer) {
        val navController = rememberNavController()
        val invitation by appContainer.invitations.pendingOpen.collectAsState()
        LaunchedEffect(invitation?.id) {
            if (invitation != null && navController.currentDestination?.hasRoute<DebugAppRoute>() == false) {
                navController.navigate(DebugAppRoute) {
                    popUpTo<DebugAppRoute>()
                    launchSingleTop = true
                }
            }
        }
        YeobaekTheme {
            NavHost(navController = navController, startDestination = DebugAppRoute) {
                composable<DebugAppRoute> {
                    DevApp(
                        appContainer = appContainer,
                        navigateToDevTools = { navController.navigate(DevToolsRoute) },
                    )
                }
                composable<DevToolsRoute> {
                    Surface {
                        DevToolsScreen(
                            appContainer = appContainer,
                            appBuild = appBuild,
                            buildType = "debug",
                            navigateBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

@Serializable
private data object DebugAppRoute

@Serializable
private data object DevToolsRoute
