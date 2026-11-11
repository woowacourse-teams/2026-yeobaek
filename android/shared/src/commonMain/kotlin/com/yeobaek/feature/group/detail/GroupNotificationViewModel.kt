package com.yeobaek.feature.group.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yeobaek.core.analytics.AnalyticsEvent
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.GroupNotificationAction
import com.yeobaek.core.analytics.GroupNotificationSettingEvent
import com.yeobaek.data.repository.NotificationRepository
import kotlin.time.TimeSource
import kotlin.uuid.Uuid
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
    private val trackEvent: (AnalyticsEvent) -> Unit = {},
) : ViewModel() {
    private val state = MutableStateFlow(GroupNotificationUiState())
    val uiState = state.asStateFlow()
    private val retryCount = MutableStateFlow(0)
    private var loadedIdentity: Pair<Int, String>? = null
    private var saveJob: Job? = null
    private val detailVisitId = Uuid.random().toString()
    private var viewed = false
    private var loadFailureReason: String? = null

    init {
        viewModelScope.launch {
            combine(memberId, installationId, retryCount, ::Triple)
                .collectLatest { (member, fid, retry) ->
                    saveJob?.cancel()
                    loadedIdentity = null
                    loadFailureReason = null
                    state.value = GroupNotificationUiState()
                    val start = TimeSource.Monotonic.markNow()
                    val trigger = if (retry == 0) "initial" else "retry"

                    if (member == null || fid.isNullOrBlank()) {
                        delay(INSTALLATION_WAIT_MILLIS)
                        loadFailureReason = "registration_timeout"
                        state.value = GroupNotificationUiState(
                            isLoading = false,
                            error = "기기 등록 정보를 가져오지 못했어요. 다시 시도해 주세요.",
                        )
                        trackLoaded(trigger, start.elapsedNow().inWholeMilliseconds)
                        return@collectLatest
                    }

                    try {
                        val enabled = repository.getGroupPreference(member, fid, groupId)
                        currentCoroutineContext().ensureActive()
                        loadedIdentity = member to fid
                        state.value = GroupNotificationUiState(enabled = enabled, isLoading = false)
                        trackLoaded(trigger, start.elapsedNow().inWholeMilliseconds, enabled)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        currentCoroutineContext().ensureActive()
                        loadFailureReason = "request_failed"
                        state.value = GroupNotificationUiState(
                            isLoading = false,
                            error = "모임 알림 설정을 가져오지 못했어요. 다시 시도해 주세요.",
                        )
                        trackLoaded(trigger, start.elapsedNow().inWholeMilliseconds)
                    }
                }
        }
    }

    fun onSettingVisible() {
        val enabled = state.value.enabled ?: return
        if (viewed) return
        viewed = true
        track(GroupNotificationAction.VIEWED, mapOf("enabled" to enabled))
    }

    fun onNotificationClick() {
        val current = state.value
        if (current.isLoading || current.isSaving) return
        val enabled = current.enabled
        if (enabled == null) {
            track(
                GroupNotificationAction.LOAD_RETRIED,
                loadFailureReason?.let { mapOf("previous_failure_reason" to it) }.orEmpty(),
            )
            state.value = GroupNotificationUiState()
            retryCount.value++
            return
        }

        val identity = loadedIdentity ?: return
        if (identity != (memberId.value to installationId.value)) return
        val (member, fid) = identity
        val attempt = mapOf<String, Any>(
            "setting_attempt_id" to Uuid.random().toString(),
            "previous_enabled" to enabled,
            "target_enabled" to !enabled,
        )
        state.value = current.copy(enabled = !enabled, isSaving = true, error = null)
        track(GroupNotificationAction.TOGGLE_CLICKED, attempt)
        saveJob = viewModelScope.launch {
            val start = TimeSource.Monotonic.markNow()
            val result = try {
                repository.updateGroupPreference(member, fid, groupId, !enabled)
                currentCoroutineContext().ensureActive()
                state.value = state.value.copy(isSaving = false)
                EventResult.SUCCESS
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                state.value = current.copy(error = "모임 알림 설정을 저장하지 못했어요. 다시 시도해 주세요.")
                EventResult.FAILURE
            }
            track(
                GroupNotificationAction.SUBMITTED,
                attempt +
                    buildMap {
                        put("result", result.value)
                        put("duration_ms", start.elapsedNow().inWholeMilliseconds)
                        if (result == EventResult.FAILURE) put("failure_reason", "request_failed")
                    },
            )
        }
    }

    fun dismissError() {
        state.value = state.value.copy(error = null)
    }

    private fun trackLoaded(trigger: String, durationMillis: Long, enabled: Boolean? = null) {
        track(
            GroupNotificationAction.LOADED,
            buildMap {
                put("trigger", trigger)
                put("duration_ms", durationMillis)
                put("result", if (enabled == null) EventResult.FAILURE.value else EventResult.SUCCESS.value)
                enabled?.let { put("enabled", it) }
                loadFailureReason?.let { put("failure_reason", it) }
            },
        )
    }

    private fun track(action: GroupNotificationAction, details: Map<String, Any>) {
        trackEvent(GroupNotificationSettingEvent(action, groupId, detailVisitId, details))
    }

    private companion object {
        const val INSTALLATION_WAIT_MILLIS = 15_000L
    }
}
