package com.yeobaek.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RecommendedUpdateStage {
    IDLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    INSTALLING,
    FAILED,
}

data class RecommendedUpdateState(
    val stage: RecommendedUpdateStage = RecommendedUpdateStage.IDLE,
    val errorMessage: String? = null,
)

class RecommendedUpdateController(
    private val manager: AppUpdateManager,
    private val currentVersionCode: Int,
    private val session: RecommendedUpdateSession,
) {
    private val mutableState = MutableStateFlow(RecommendedUpdateState())
    val state = mutableState.asStateFlow()
    private var closed = false
    private var foreground = false
    private var queryGeneration = 0
    private var flowInFlight = false
    private var completing = false
    private val listener = InstallStateUpdatedListener { install ->
        if (!closed) {
            queryGeneration++
            updateInstallState(install.installStatus())
        }
    }

    init {
        manager.registerListener(listener)
    }

    fun resume(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        foreground = true
        checkUpdate(launcher)
    }

    fun pause() {
        foreground = false
        queryGeneration++
    }

    fun onResult(resultCode: Int) {
        if (closed) return
        flowInFlight = false
        if (resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
            showFailure("업데이트를 시작하지 못했어요.")
        }
    }

    fun install() {
        if (closed || completing || state.value.stage != RecommendedUpdateStage.READY_TO_INSTALL) return
        completing = true
        queryGeneration++
        mutableState.value = state.value.copy(stage = RecommendedUpdateStage.INSTALLING, errorMessage = null)
        manager.completeUpdate().addOnFailureListener {
            if (!closed) {
                completing = false
                mutableState.value = state.value.copy(
                    stage = RecommendedUpdateStage.READY_TO_INSTALL,
                    errorMessage = "업데이트를 적용하지 못했어요.",
                )
            }
        }
    }

    fun consumeError() {
        mutableState.value = if (state.value.stage == RecommendedUpdateStage.FAILED) {
            RecommendedUpdateState()
        } else {
            state.value.copy(errorMessage = null)
        }
    }

    fun close() {
        if (closed) return
        closed = true
        queryGeneration++
        manager.unregisterListener(listener)
    }

    private fun checkUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (closed) return
        val generation = ++queryGeneration
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (closed || !foreground || generation != queryGeneration) return@addOnSuccessListener
            updateInstallState(info.installStatus())
            if (flowInFlight ||
                state.value.stage == RecommendedUpdateStage.DOWNLOADING ||
                state.value.stage == RecommendedUpdateStage.READY_TO_INSTALL ||
                state.value.stage == RecommendedUpdateStage.INSTALLING
            ) {
                return@addOnSuccessListener
            }
            val versionCode = info.availableVersionCode()
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                versionCode > currentVersionCode &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                session.recordAvailableVersion(versionCode)
                if (session.canPrompt(versionCode)) startUpdate(info, launcher)
            }
        }
    }

    private fun startUpdate(info: AppUpdateInfo, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        flowInFlight = true
        val started = try {
            manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
            )
        } catch (_: Exception) {
            false
        }
        if (started) {
            session.markPrompted(info.availableVersionCode())
            if (state.value.stage == RecommendedUpdateStage.FAILED) mutableState.value = RecommendedUpdateState()
        } else {
            flowInFlight = false
            showFailure("업데이트를 시작하지 못했어요.")
        }
    }

    private fun updateInstallState(status: Int) {
        when (status) {
            InstallStatus.PENDING, InstallStatus.DOWNLOADING -> {
                mutableState.value = RecommendedUpdateState(stage = RecommendedUpdateStage.DOWNLOADING)
            }

            InstallStatus.DOWNLOADED -> {
                if (!completing) mutableState.value = state.value.copy(stage = RecommendedUpdateStage.READY_TO_INSTALL)
            }

            InstallStatus.INSTALLING -> {
                completing = true
                mutableState.value = state.value.copy(stage = RecommendedUpdateStage.INSTALLING, errorMessage = null)
            }

            InstallStatus.FAILED -> {
                completing = false
                flowInFlight = false
                showFailure("업데이트를 다운로드하지 못했어요.")
            }

            InstallStatus.CANCELED, InstallStatus.INSTALLED -> {
                completing = false
                flowInFlight = false
                mutableState.value = RecommendedUpdateState()
            }
        }
    }

    private fun showFailure(message: String) {
        mutableState.value = RecommendedUpdateState(RecommendedUpdateStage.FAILED, errorMessage = message)
    }
}
