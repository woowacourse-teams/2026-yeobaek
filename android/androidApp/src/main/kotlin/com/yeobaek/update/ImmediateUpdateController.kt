package com.yeobaek.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RequiredUpdateActionState {
    data object Preparing : RequiredUpdateActionState

    data class Ready(val errorMessage: String? = null) : RequiredUpdateActionState

    data object Processing : RequiredUpdateActionState
}

class ImmediateUpdateController(
    private val manager: AppUpdateManager,
    private val currentVersionCode: Int,
    private val minimumVersionCode: () -> Int,
    private val openStore: () -> Boolean,
    private val refreshPolicy: () -> Unit,
) {
    private val mutableState = MutableStateFlow<RequiredUpdateActionState>(RequiredUpdateActionState.Preparing)
    val state = mutableState.asStateFlow()
    private var querying = false
    private var closed = false

    fun start(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (state.value !is RequiredUpdateActionState.Ready) return
        checkUpdate(launcher, userRequested = true)
    }

    fun resume(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        checkUpdate(launcher, userRequested = false)
    }

    fun onResult(resultCode: Int) {
        mutableState.value = RequiredUpdateActionState.Ready(
            errorMessage = if (resultCode == Activity.RESULT_OK) null else "업데이트를 완료해야 계속 이용할 수 있어요.",
        )
        refreshPolicy()
    }

    fun close() {
        closed = true
    }

    private fun checkUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>, userRequested: Boolean) {
        if (closed || querying) return
        querying = true
        mutableState.value = when (state.value) {
            RequiredUpdateActionState.Preparing -> RequiredUpdateActionState.Preparing
            is RequiredUpdateActionState.Ready -> RequiredUpdateActionState.Processing
            RequiredUpdateActionState.Processing -> RequiredUpdateActionState.Processing
        }
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (closed) return@addOnSuccessListener
            querying = false
            val inProgress = info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            val available = canStartImmediateUpdate(info)
            if (inProgress || (userRequested && available)) {
                val started = try {
                    manager.startUpdateFlowForResult(
                        info,
                        launcher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                    )
                } catch (_: Exception) {
                    false
                }
                if (!started) showStore()
            } else if (userRequested) {
                showStore()
            } else {
                mutableState.value = RequiredUpdateActionState.Ready()
            }
        }.addOnFailureListener {
            if (closed) return@addOnFailureListener
            querying = false
            if (userRequested) showStore() else mutableState.value = RequiredUpdateActionState.Ready()
        }
    }

    private fun canStartImmediateUpdate(info: AppUpdateInfo): Boolean =
        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.availableVersionCode() >= minimumVersionCode() &&
            info.availableVersionCode() > currentVersionCode &&
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

    private fun showStore() {
        val opened = openStore()
        mutableState.value = RequiredUpdateActionState.Ready(
            errorMessage = if (opened) null else "스토어를 열지 못했어요. 잠시 후 다시 시도해 주세요.",
        )
    }
}
