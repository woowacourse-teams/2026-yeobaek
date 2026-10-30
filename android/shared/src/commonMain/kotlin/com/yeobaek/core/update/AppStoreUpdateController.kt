package com.yeobaek.core.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class AppStoreUpdateState(
    val offeredVersion: String? = null,
    val isOpeningStore: Boolean = false,
    val errorMessage: String? = null,
)

internal class AppStoreUpdateController(
    private val policyRepository: UpdatePolicyRepository,
    private val session: AppStoreUpdateSession,
    private val currentVersion: String,
    private val currentVersionCode: Int,
    private val latestVersion: suspend () -> String?,
    private val openAppStore: suspend () -> Boolean,
) {
    private val mutableState = MutableStateFlow(AppStoreUpdateState())
    val state = mutableState.asStateFlow()

    suspend fun checkForUpdate() {
        policyRepository.awaitFirstRefresh()
        if (session.hasCheckedStore || isUpdateRequired()) return

        try {
            val version = latestVersion()
            if (isUpdateRequired()) return
            session.markStoreChecked()
            if (version != null && session.canPrompt(version, currentVersion)) {
                mutableState.value = state.value.copy(offeredVersion = version)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // 권장 업데이트 조회 실패는 앱 이용을 막지 않는다.
        }
    }

    fun markPrompted(version: String) {
        session.markPrompted(version)
    }

    fun clearOffer() {
        mutableState.value = state.value.copy(offeredVersion = null)
    }

    fun dismiss() {
        if (!state.value.isOpeningStore) clearOffer()
    }

    suspend fun openStore() {
        if (state.value.isOpeningStore || state.value.offeredVersion == null) return
        mutableState.value = state.value.copy(isOpeningStore = true, errorMessage = null)
        try {
            if (openAppStore()) {
                clearOffer()
            } else {
                mutableState.value = state.value.copy(
                    errorMessage = "스토어를 열지 못했어요. 잠시 후 다시 시도해 주세요.",
                )
            }
        } finally {
            mutableState.value = state.value.copy(isOpeningStore = false)
        }
    }

    private fun isUpdateRequired(): Boolean = policyRepository.policy.value.isUpdateRequired(currentVersionCode)
}
