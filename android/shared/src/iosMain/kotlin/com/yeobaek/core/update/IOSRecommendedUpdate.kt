package com.yeobaek.core.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekSurfaceElevated
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@Composable
internal fun IOSRecommendedUpdate(
    appContainer: AppContainer,
    updatesAllowed: Boolean,
    content: @Composable () -> Unit,
) {
    val source = remember { IOSAppStoreVersionSource() }
    val controller = remember(appContainer, source) {
        AppStoreUpdateController(
            policyRepository = appContainer.updatePolicyRepository,
            session = requireNotNull(appContainer.appStoreUpdateSession),
            currentVersion = appContainer.appVersion,
            currentVersionCode = appContainer.appVersionCode,
            latestVersion = source::latestVersion,
            openAppStore = IOSAppStore::open,
        )
    }
    val state by controller.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    DisposableEffect(source) { onDispose { source.close() } }
    LaunchedEffect(controller, lifecycleOwner, updatesAllowed) {
        if (!updatesAllowed) {
            controller.clearOffer()
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            controller.checkForUpdate()
        }
    }

    content()
    val version = state.offeredVersion
    if (updatesAllowed && version != null) {
        LaunchedEffect(controller, version) { controller.markPrompted(version) }
        YeobaekTheme {
            AlertDialog(
                onDismissRequest = controller::dismiss,
                shape = RoundedCornerShape(15.dp),
                containerColor = YeobaekSurfaceElevated,
                title = { Text("새 버전이 있어요", style = MaterialTheme.typography.titleMedium, color = YeobaekTextStrong) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UpdateMessageText(
                            message = "더 안정적인 이용을 위해 최신 버전으로 업데이트해 주세요.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        state.errorMessage?.let {
                            UpdateMessageText(
                                message = it,
                                color = MaterialTheme.colorScheme.error,
                                liveRegionMode = LiveRegionMode.Polite,
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !state.isOpeningStore,
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                controller.openStore()
                            }
                        },
                    ) { Text(if (state.isOpeningStore) "스토어 여는 중" else "업데이트") }
                },
                dismissButton = {
                    TextButton(enabled = !state.isOpeningStore, onClick = controller::dismiss) { Text("나중에") }
                },
            )
        }
    }
}
