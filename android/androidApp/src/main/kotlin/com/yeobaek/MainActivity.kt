package com.yeobaek

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.yeobaek.core.app.AppContainer
import com.yeobaek.data.local.readingprogress.createReadingProgressStore
import com.yeobaek.update.AndroidRequiredUpdateGate
import java.util.UUID

class MainActivity : ComponentActivity() {

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
                App(appContainer = appContainer)
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

@Preview
@Composable
fun AppAndroidPreview() {
    val context = LocalContext.current
    val container = remember {
        AppContainer(
            isDebug = true,
            appVersion = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            installationId = UUID.randomUUID().toString(),
            readingProgressStore = createReadingProgressStore(context),
        )
    }
    DisposableEffect(container) { onDispose { container.close() } }
    App(appContainer = container)
}
