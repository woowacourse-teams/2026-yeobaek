package com.yeobaek.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.yeobaek.core.app.AppContainer
import kotlinx.coroutines.launch

@Composable
fun IOSRequiredUpdateGate(
    appContainer: AppContainer,
    content: @Composable () -> Unit,
) {
    val policy by appContainer.updatePolicyRepository.policy.collectAsState()
    var isOpeningStore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    RequiredUpdateGate(
        policy = policy,
        currentVersionCode = appContainer.appVersionCode,
        onUpdate = {
            if (!isOpeningStore) {
                isOpeningStore = true
                errorMessage = null
                scope.launch {
                    try {
                        if (!IOSAppStore.open()) {
                            errorMessage = "스토어를 열지 못했어요. 잠시 후 다시 시도해 주세요."
                        }
                    } finally {
                        isOpeningStore = false
                    }
                }
            }
        },
        isUpdating = isOpeningStore,
        errorMessage = errorMessage,
    ) {
        IOSRecommendedUpdate(
            appContainer = appContainer,
            updatesAllowed = !policy.isUpdateRequired(appContainer.appVersionCode),
            content = content,
        )
    }
}
