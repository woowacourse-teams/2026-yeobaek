package com.yeobaek

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yeobaek.update.AndroidRequiredUpdateGate
import kotlinx.serialization.Serializable

class DebugMainActivity : ComponentActivity() {

    private val notificationPermissionRequester = NotificationPermissionRequester(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appContainer = (application as YeobaekApplication).appContainer
        intent.deliverNotificationOpen(appContainer)

        setContent {
            DoubleBackToExit(this) {
                finish()
            }
            AndroidRequiredUpdateGate(appContainer) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = App,
                ) {
                    composable<App> {
                        DevApp(
                            appContainer = appContainer,
                            navigateToDevTools = {
                                navController.navigate(DevTools)
                            },
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
        }

        notificationPermissionRequester.requestOnce()
    }

    override fun onResume() {
        super.onResume()
        (application as YeobaekApplication).appContainer.refreshUpdatePolicy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.deliverNotificationOpen((application as YeobaekApplication).appContainer)
    }
}

@Serializable
data object App

@Serializable
data object DevTools
