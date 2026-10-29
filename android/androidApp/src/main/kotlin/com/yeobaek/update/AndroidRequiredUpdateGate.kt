package com.yeobaek.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekInk
import com.yeobaek.core.designsystem.theme.YeobaekPaper
import com.yeobaek.core.update.RequiredUpdateGate
import com.yeobaek.core.update.UpdatePolicy

@Composable
fun AndroidRequiredUpdateGate(
    appContainer: AppContainer,
    recommendedUpdateSession: RecommendedUpdateSession,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val manager = remember(context) { AppUpdateManagerFactory.create(context) }
    val repository = appContainer.updatePolicyRepository
    val policy by repository.policy.collectAsState()
    val controller = remember(appContainer, context, manager) {
        ImmediateUpdateController(
            manager = manager,
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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        controller.resume(launcher)
    }
    DisposableEffect(controller) {
        onDispose { controller.close() }
    }
    AndroidRequiredUpdateContent(
        policy = policy,
        currentVersionCode = appContainer.appVersionCode,
        onUpdate = { controller.start(launcher) },
        actionState = actionState,
        content = {
            AndroidRecommendedUpdate(
                manager,
                appContainer.appVersionCode,
                recommendedUpdateSession,
                repository,
                content,
            )
        },
    )
}

@Composable
internal fun AndroidRequiredUpdateContent(
    policy: UpdatePolicy,
    currentVersionCode: Int,
    actionState: RequiredUpdateActionState,
    onUpdate: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (policy.isUpdateRequired(currentVersionCode) && !actionState.isPrepared) {
        BackHandler {}
        Box(
            Modifier.fillMaxSize().background(YeobaekPaper).background(YeobaekInk.copy(alpha = 0.36f)),
        )
        return
    }
    RequiredUpdateGate(
        policy = policy,
        currentVersionCode = currentVersionCode,
        onUpdate = onUpdate,
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
