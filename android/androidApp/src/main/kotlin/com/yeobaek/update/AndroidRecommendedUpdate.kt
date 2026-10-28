package com.yeobaek.update

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.play.core.appupdate.AppUpdateManager
import com.yeobaek.core.designsystem.theme.YeobaekSurfaceElevated
import com.yeobaek.core.designsystem.theme.YeobaekTheme

@Composable
internal fun AndroidRecommendedUpdate(
    manager: AppUpdateManager,
    currentVersionCode: Int,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(manager, currentVersionCode, context) {
        RecommendedUpdateController(
            manager,
            currentVersionCode,
            RecommendedUpdatePreferences(context.getSharedPreferences("recommendedUpdates", Context.MODE_PRIVATE)),
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        controller.onResult(result.resultCode)
    }
    val state by controller.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { controller.resume(launcher) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { controller.pause() }
    LaunchedEffect(controller, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            controller.state.collect { update ->
                val message = update.errorMessage ?: return@collect
                controller.consumeError()
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            }
        }
    }
    DisposableEffect(controller) {
        onDispose { controller.close() }
    }
    YeobaekTheme {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            val readyToInstall = state.stage == RecommendedUpdateStage.READY_TO_INSTALL
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 16.dp).then(
                    if (snackbarHostState.currentSnackbarData != null && !readyToInstall) {
                        Modifier.navigationBarsPadding()
                    } else {
                        Modifier
                    },
                ),
            )
            if (readyToInstall) {
                RecommendedUpdateBanner(onInstall = controller::install)
            }
        }
    }
}

@Composable
internal fun RecommendedUpdateBanner(onInstall: () -> Unit) {
    Surface(color = YeobaekSurfaceElevated, tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("새 버전 설치 준비가 끝났어요", style = MaterialTheme.typography.titleSmall)
            Text("재시작하면 업데이트가 적용돼요.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onInstall) { Text("지금 재시작") }
        }
    }
}

@Preview(name = "재시작 안내", group = "권장 업데이트", widthDp = 390, showBackground = true)
@Composable
private fun RecommendedUpdateReadyPreview() {
    YeobaekTheme {
        RecommendedUpdateBanner(onInstall = {})
    }
}

@Preview(name = "실패 알림", group = "권장 업데이트", widthDp = 390, showBackground = true)
@Composable
private fun RecommendedUpdateFailedPreview() {
    YeobaekTheme {
        Box(Modifier.padding(16.dp)) {
            Snackbar {
                Text("업데이트를 다운로드하지 못했어요.")
            }
        }
    }
}
