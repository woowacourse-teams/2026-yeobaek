package com.yeobaek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

class DebugMainActivity : ComponentActivity() {

    private val notificationPermissionRequester = NotificationPermissionRequester(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appContainer = (application as YeobaekApplication).appContainer

        setContent {
            val navController = rememberNavController()

            DoubleBackToExit(this) {
                finish()
            }
            NavHost(
                navController = navController,
                startDestination = App,
            ) {
                composable<App> {
                    DevApp(
                        appContainer = appContainer,
                        navigateToDevTools = {
                            navController.navigate(DevTools)
                        }
                    )
                }
                composable<DevTools> {
                    DevToolsScreen(
                        appVersion = appContainer.appVersion,
                        buildType = BuildConfig.BUILD_TYPE,
                        serverEnv = appContainer.apiBaseUrl,
                    )
                }
            }
        }

        notificationPermissionRequester.requestOnce()
    }
}

@Serializable
data object App

@Serializable
data object DevTools
