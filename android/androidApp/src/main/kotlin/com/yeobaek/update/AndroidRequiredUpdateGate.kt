package com.yeobaek.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.update.RequiredUpdateGate
import com.yeobaek.core.update.UpdatePolicy

@Composable
fun AndroidRequiredUpdateGate(
    appContainer: AppContainer,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val repository = appContainer.updatePolicyRepository
    var policy by remember(appContainer) {
        mutableStateOf<UpdatePolicy?>(repository.policy.value.takeIf { it.isUpdateRequired(appContainer.appVersionCode) })
    }
    val controller = remember(appContainer, context) {
        ImmediateUpdateController(
            manager = AppUpdateManagerFactory.create(context),
            currentVersionCode = appContainer.appVersionCode,
            minimumVersionCode = { repository.policy.value.minSupportedVersionCode },
            openStore = { openPlayStore(context) },
            refreshPolicy = appContainer::refreshUpdatePolicy,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        controller.onResult(result.resultCode)
    }
    val actionState by controller.state.collectAsState()
    LaunchedEffect(repository) {
        repository.awaitFirstRefresh()
        repository.policy.collect { policy = it }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        controller.resume(launcher)
    }
    DisposableEffect(controller) {
        onDispose { controller.close() }
    }
    RequiredUpdateGate(
        policy = policy,
        currentVersionCode = appContainer.appVersionCode,
        onUpdate = { controller.start(launcher) },
        isUpdating = actionState.isBusy,
        errorMessage = actionState.errorMessage,
        content = content,
    )
}

private fun openPlayStore(context: Context): Boolean {
    val intents = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.yeobaek")).setPackage("com.android.vending"),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.yeobaek")),
    )
    return intents.any { intent ->
        try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
