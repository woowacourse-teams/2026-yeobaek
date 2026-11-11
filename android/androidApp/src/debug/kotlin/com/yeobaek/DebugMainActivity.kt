package com.yeobaek

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yeobaek.update.AndroidRequiredUpdateGate
import kotlinx.serialization.Serializable

class DebugMainActivity : ComponentActivity() {

    private var invitationRequestId: String? = null

    private val notificationPermissionRequester = NotificationPermissionRequester(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as YeobaekApplication
        val appContainer = app.appContainer
        intent.deliverNotificationOpen(appContainer)
        invitationRequestId = intent.deliverInvitation(
            appContainer.invitations,
            savedInstanceState?.getString(INVITATION_REQUEST_ID),
        )

        setContent {
            DoubleBackToExit(this) {
                finish()
            }
            AndroidRequiredUpdateGate(appContainer, app.recommendedUpdateSession) {
                val navController = rememberNavController()
                val invitation by appContainer.invitations.pendingOpen.collectAsState()
                LaunchedEffect(invitation?.id) {
                    if (invitation != null && navController.currentDestination?.hasRoute<App>() == false) {
                        navController.navigate(App) {
                            popUpTo<App>()
                            launchSingleTop = true
                        }
                    }
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
                            },
                        )
                    }
                    composable<DevTools> { entry ->
                        DevTools(
                            appContainer = appContainer,
                            navigateBack = { navController.popBackStackIfCurrent(entry.id) },
                        )
                    }
                }
            }
        }

        notificationPermissionRequester.requestOnce()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(INVITATION_REQUEST_ID, invitationRequestId)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val appContainer = (application as YeobaekApplication).appContainer
        intent.deliverNotificationOpen(appContainer)
        invitationRequestId = intent.deliverInvitation(appContainer.invitations)
    }
}

@Serializable
data object App

@Serializable
data object DevTools
