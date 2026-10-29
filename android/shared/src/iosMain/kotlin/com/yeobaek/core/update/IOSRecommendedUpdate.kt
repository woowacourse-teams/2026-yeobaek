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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.yeobaek.core.app.AppContainer
import com.yeobaek.core.designsystem.theme.YeobaekSurfaceElevated
import com.yeobaek.core.designsystem.theme.YeobaekTextStrong
import com.yeobaek.core.designsystem.theme.YeobaekTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun IOSRecommendedUpdate(
    appContainer: AppContainer,
    updatesAllowed: Boolean,
    content: @Composable () -> Unit,
) {
    val session = requireNotNull(appContainer.appStoreUpdateSession)
    val source = remember { IOSAppStoreVersionSource() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var offeredVersion by remember { mutableStateOf<String?>(null) }
    var isOpeningStore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    DisposableEffect(source) { onDispose { source.close() } }
    LaunchedEffect(appContainer, lifecycleOwner, updatesAllowed) {
        if (!updatesAllowed) {
            offeredVersion = null
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            appContainer.updatePolicyRepository.awaitFirstRefresh()
            if (session.hasCheckedStore ||
                appContainer.updatePolicyRepository.policy.value.isUpdateRequired(appContainer.appVersionCode)
            ) {
                return@repeatOnLifecycle
            }
            try {
                val version = source.latestVersion()
                if (appContainer.updatePolicyRepository.policy.value.isUpdateRequired(appContainer.appVersionCode)) {
                    return@repeatOnLifecycle
                }
                session.markStoreChecked()
                if (version != null && session.canPrompt(version, appContainer.appVersion)) offeredVersion = version
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // 권장 업데이트 조회 실패는 앱 이용을 막지 않는다.
            }
        }
    }

    content()
    val version = offeredVersion
    if (updatesAllowed && version != null) {
        LaunchedEffect(version) { session.markPrompted(version) }
        YeobaekTheme {
            AlertDialog(
                onDismissRequest = { if (!isOpeningStore) offeredVersion = null },
                shape = RoundedCornerShape(15.dp),
                containerColor = YeobaekSurfaceElevated,
                title = { Text("새 버전이 있어요", style = MaterialTheme.typography.titleMedium, color = YeobaekTextStrong) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UpdateMessageText(
                            message = "더 안정적인 이용을 위해 최신 버전으로 업데이트해 주세요.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        errorMessage?.let {
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
                        enabled = !isOpeningStore,
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            isOpeningStore = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    if (IOSAppStore.open()) {
                                        offeredVersion = null
                                    } else {
                                        errorMessage = "스토어를 열지 못했어요. 잠시 후 다시 시도해 주세요."
                                    }
                                } finally {
                                    isOpeningStore = false
                                }
                            }
                        },
                    ) { Text(if (isOpeningStore) "스토어 여는 중" else "업데이트") }
                },
                dismissButton = {
                    TextButton(enabled = !isOpeningStore, onClick = { offeredVersion = null }) { Text("나중에") }
                },
            )
        }
    }
}
