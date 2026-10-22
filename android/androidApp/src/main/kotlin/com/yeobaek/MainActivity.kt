package com.yeobaek

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.app.AppContainer

class MainActivity : ComponentActivity() {

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
            App(
                appContainer = appContainer,
            )
        }

        notificationPermissionRequester.requestOnce()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.deliverNotificationOpen((application as YeobaekApplication).appContainer)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        appContainer = AppContainer(
            isDebug = false,
            appVersion = "1.0.0",
        ),
    )
}
