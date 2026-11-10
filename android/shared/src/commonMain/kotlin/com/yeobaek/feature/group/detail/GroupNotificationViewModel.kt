package com.yeobaek.feature.group.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeobaek.data.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GroupNotificationUiState(
    val enabled: Boolean? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
)

class GroupNotificationViewModel(
    private val groupId: Long,
    private val repository: NotificationRepository,
    private val memberId: StateFlow<Int?>,
    private val installationId: StateFlow<String?>,
) : ViewModel() {
    private val state = MutableStateFlow(GroupNotificationUiState())
    val uiState = state.asStateFlow()
    private val retryCount = MutableStateFlow(0)
    private var loadedIdentity: Pair<Int, String>? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            combine(memberId, installationId, retryCount) { member, fid, _ -> member to fid }
                .collectLatest { (member, fid) ->
                    saveJob?.cancel()
                    loadedIdentity = null
                    state.value = GroupNotificationUiState()

                    if (member == null || fid.isNullOrBlank()) {
                        delay(INSTALLATION_WAIT_MILLIS)
                        state.value = GroupNotificationUiState(
                            isLoading = false,
                            error = "기기 등록 정보를 가져오지 못했어요. 다시 시도해 주세요.",
                        )
                        return@collectLatest
                    }

                    try {
                        val enabled = repository.getGroupPreference(member, fid, groupId)
                        currentCoroutineContext().ensureActive()
                        loadedIdentity = member to fid
                        state.value = GroupNotificationUiState(enabled = enabled, isLoading = false)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        state.value = GroupNotificationUiState(
                            isLoading = false,
                            error = "모임 알림 설정을 가져오지 못했어요. 다시 시도해 주세요.",
                        )
                    }
                }
        }
    }

    fun onNotificationClick() {
        val current = state.value
        if (current.isLoading || current.isSaving) return
        val enabled = current.enabled
        if (enabled == null) {
            state.value = GroupNotificationUiState()
            retryCount.value++
            return
        }

        val identity = loadedIdentity ?: return
        if (identity != (memberId.value to installationId.value)) return
        val (member, fid) = identity
        state.value = current.copy(enabled = !enabled, isSaving = true, error = null)
        saveJob = viewModelScope.launch {
            try {
                repository.updateGroupPreference(member, fid, groupId, !enabled)
                currentCoroutineContext().ensureActive()
                state.value = state.value.copy(isSaving = false)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                state.value = current.copy(error = "모임 알림 설정을 저장하지 못했어요. 다시 시도해 주세요.")
            }
        }
    }

    fun dismissError() {
        state.value = state.value.copy(error = null)
    }

    private companion object {
        const val INSTALLATION_WAIT_MILLIS = 15_000L
    }
}
