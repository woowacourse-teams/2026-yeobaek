package com.yeobaek.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.NotificationSettingSubmitted
import com.yeobaek.data.model.NotificationPreferences
import com.yeobaek.data.model.NotificationSetting
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

data class NotificationSettingsUiState(
    val preferences: NotificationPreferences? = null,
    val isLoading: Boolean = true,
    val savingSettings: Set<NotificationSetting> = emptySet(),
    val loadError: String? = null,
    val saveError: String? = null,
) {
    val isSaving: Boolean get() = savingSettings.isNotEmpty()
}

class NotificationSettingsViewModel(
    private val repository: NotificationRepository,
    private val memberId: StateFlow<Int?>,
    private val installationId: StateFlow<String?>,
    private val trackEvent: (AnalyticsEvent) -> Unit = {},
) : ViewModel() {
    private val state = MutableStateFlow(NotificationSettingsUiState())
    val uiState = state.asStateFlow()
    private val refreshVersion = MutableStateFlow(0)
    private var loadedIdentity: Pair<Int, String>? = null
    private val saveJobs = mutableMapOf<NotificationSetting, Job>()
    private var hasResumed = false
    private var mutationVersion = 0L

    init {
        viewModelScope.launch {
            combine(memberId, installationId, refreshVersion) { member, fid, _ -> member to fid }
                .collectLatest { (member, fid) ->
                    saveJobs.values.toList().forEach { it.cancel() }
                    saveJobs.clear()
                    val cached = state.value.preferences.takeIf { loadedIdentity == (member to fid) }
                    if (cached == null) loadedIdentity = null
                    state.value = NotificationSettingsUiState(preferences = cached, isLoading = cached == null)
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
                    val version = mutationVersion
                    try {
                        val preferences = repository.getPreferences(member, fid)
                        currentCoroutineContext().ensureActive()
                        if (version != mutationVersion) return@collectLatest
                        loadedIdentity = member to fid
                        state.value = NotificationSettingsUiState(preferences = preferences, isLoading = false)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        if (version != mutationVersion) return@collectLatest
                        state.value = NotificationSettingsUiState(
                            preferences = cached,
                            isLoading = false,
                            loadError = "알림 설정을 가져오지 못했습니다.",
                        )
                    }
                }
        }
    }

    fun onScreenResumed() {
        if (hasResumed) refresh()
        hasResumed = true
    }

    fun refresh() {
        if (!state.value.isSaving) refreshVersion.value++
    }

    fun update(setting: NotificationSetting, enabled: Boolean) {
        val current = state.value
        val preferences = current.preferences ?: return
        val identity = loadedIdentity ?: return
        if (identity != (memberId.value to installationId.value) || current.isLoading) return
        if (setting in current.savingSettings) return
        if (setting != NotificationSetting.ALL && !preferences.enabled) return
        if (preferences.isEnabled(setting) == enabled) return

        mutationVersion++
        state.value = current.copy(
            preferences = preferences.updated(setting, enabled),
            savingSettings = current.savingSettings + setting,
            saveError = null,
        )
        val job = viewModelScope.launch {
            try {
                repository.updatePreferences(identity.first, identity.second, setting, enabled)
                trackEvent(NotificationSettingSubmitted(setting, enabled, EventResult.SUCCESS))
                if (loadedIdentity == identity) {
                    state.value = state.value.copy(savingSettings = state.value.savingSettings - setting)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                trackEvent(NotificationSettingSubmitted(setting, enabled, EventResult.FAILURE))
                if (loadedIdentity == identity) {
                    state.value = state.value.copy(
                        preferences = state.value.preferences?.updated(setting, preferences.isEnabled(setting)),
                        savingSettings = state.value.savingSettings - setting,
                        saveError = "알림 설정을 저장하지 못했습니다. 다시 시도해 주세요.",
                    )
                }
            }
        }
        saveJobs[setting] = job
        job.invokeOnCompletion {
            if (saveJobs[setting] === job) saveJobs.remove(setting)
        }
    }

    fun dismissSaveError() {
        state.value = state.value.copy(saveError = null)
    }

    private companion object {
        const val INSTALLATION_WAIT_MILLIS = 15_000L
    }
}
