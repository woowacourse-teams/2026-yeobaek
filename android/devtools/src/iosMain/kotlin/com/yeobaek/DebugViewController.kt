package com.yeobaek

import androidx.compose.material3.Surface
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import com.yeobaek.devtools.DevApp
import com.yeobaek.devtools.DevToolsScreen
import kotlinx.serialization.Serializable

@Suppress("FunctionName")
fun DebugViewController(
    appContainer: AppContainer,
    appBuild: String,
) = ComposeUIViewController {
    val navController = rememberNavController()
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

@Serializable
private data object DebugAppRoute

@Serializable
private data object DevToolsRoute
