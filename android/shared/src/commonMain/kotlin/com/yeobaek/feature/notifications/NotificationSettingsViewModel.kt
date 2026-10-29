package com.yeobaek.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeobaek.data.model.NotificationPreferences
import com.yeobaek.data.model.NotificationSetting
import com.yeobaek.data.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val preferences: NotificationPreferences? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
)

class NotificationSettingsViewModel(
    private val repository: NotificationRepository,
    private val memberId: StateFlow<Int?>,
    private val installationId: StateFlow<String?>,
) : ViewModel() {
    private val state = MutableStateFlow(NotificationSettingsUiState())
    val uiState = state.asStateFlow()
    private val refreshVersion = MutableStateFlow(0)
    private var loadedIdentity: Pair<Int, String>? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            combine(memberId, installationId, refreshVersion) { member, fid, _ -> member to fid }
                .collectLatest { (member, fid) ->
                    saveJob?.cancel()
                    loadedIdentity = null
                    state.value = NotificationSettingsUiState()
                    if (member == null) {
                        state.value = NotificationSettingsUiState(isLoading = false, loadError = "회원 정보를 확인해 주세요.")
                        return@collectLatest
                    }
                    if (fid.isNullOrBlank()) {
                        delay(INSTALLATION_WAIT_MILLIS)
                        state.value = NotificationSettingsUiState(
                            isLoading = false,
                            loadError = "기기 등록 정보를 가져오지 못했습니다. 다시 시도해 주세요.",
                        )
                        return@collectLatest
                    }
                    try {
                        val preferences = repository.getPreferences(member, fid)
                        loadedIdentity = member to fid
                        state.value = NotificationSettingsUiState(preferences = preferences, isLoading = false)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        state.value = NotificationSettingsUiState(
                            isLoading = false,
                            loadError = "알림 설정을 가져오지 못했습니다.",
                        )
                    }
                }
        }
    }

    fun refresh() {
        if (!state.value.isSaving) refreshVersion.value++
    }

    fun update(setting: NotificationSetting, enabled: Boolean) {
        val current = state.value
        val preferences = current.preferences ?: return
        val identity = loadedIdentity ?: return
        if (identity != (memberId.value to installationId.value) || current.isLoading || current.isSaving) return
        if (setting != NotificationSetting.ALL && !preferences.enabled) return
        if (preferences.isEnabled(setting) == enabled) return

        state.value =
            current.copy(preferences = preferences.updated(setting, enabled), isSaving = true, saveError = null)
        saveJob = viewModelScope.launch {
            try {
                repository.updatePreferences(identity.first, identity.second, setting, enabled)
                if (loadedIdentity == identity) state.value = state.value.copy(isSaving = false)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (loadedIdentity == identity) {
                    state.value = state.value.copy(
                        preferences = preferences,
                        isSaving = false,
                        saveError = "알림 설정을 저장하지 못했습니다. 다시 시도해 주세요.",
                    )
                }
            }
        }
    }

    fun dismissSaveError() {
        state.value = state.value.copy(saveError = null)
    }

    private companion object {
        const val INSTALLATION_WAIT_MILLIS = 15_000L
    }
}
