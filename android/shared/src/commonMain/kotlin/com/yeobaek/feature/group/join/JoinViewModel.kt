package com.yeobaek.feature.group.join

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.GroupJoinAbandoned
import com.yeobaek.core.analytics.GroupJoinInitiated
import com.yeobaek.core.analytics.GroupJoinSubmitted
import com.yeobaek.core.analytics.GroupJoinVisit
import com.yeobaek.core.analytics.InvalidReason
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.repository.GroupRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class JoinViewModel(
    private val visit: GroupJoinVisit,
    private val initialCode: String?,
    private val groupRepository: GroupRepository,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
    private val savedStateHandle: SavedStateHandle,
    private val onJoiningChanged: (Boolean) -> Unit,
) : ViewModel() {

    private val input = JoinInputState(savedStateHandle)
    val uiState: JoinUiState get() = input.value

    fun enter() {
        input.initialize(initialCode, visit.invitationRequestId)
        if (savedStateHandle.get<Boolean>(VISIT_RECORDED_KEY) != true) {
            savedStateHandle[VISIT_RECORDED_KEY] = true
            analyticsTracker.track(GroupJoinInitiated(visit))
        }
    }

    fun onCodeValueChange(value: String) = input.edit(value)

    fun abandon() {
        analyticsTracker.track(
            GroupJoinAbandoned(
                hasCode = uiState.codeValue.isNotBlank(),
                visit = visit,
                codeChanged = initialCode?.let { uiState.codeValue != it },
            ),
        )
    }

    fun joinGroup() {
        if (uiState.isJoining || uiState.successJoin) return

        val codeChanged = initialCode?.let { uiState.codeValue != it }
        val code = input.beginJoin()
        if (code == null) {
            val reason = if (uiState.codeValue.isBlank()) {
                InvalidReason.CODE_BLANK
            } else {
                InvalidReason.CODE_INVALID_FORMAT
            }
            analyticsTracker.track(GroupJoinSubmitted(EventResult.INVALID, visit, reason, codeChanged))
            return
        }
        onJoiningChanged(true)
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = crashContext(CrashOperation.GROUP_JOIN_STARTED),
        )
        viewModelScope.launch {
            try {
                groupRepository.joinGroup(
                    joinCode = code,
                )

                input.finishJoin(success = true)
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = crashContext(CrashOperation.GROUP_JOIN_SUCCEEDED),
                )
                analyticsTracker.track(GroupJoinSubmitted(EventResult.SUCCESS, visit, codeChanged = codeChanged))
            } catch (e: CancellationException) {
                input.cancelJoin()
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.GROUP_JOIN_FAILED),
                )
                analyticsTracker.track(GroupJoinSubmitted(EventResult.FAILURE, visit, codeChanged = codeChanged))
                input.finishJoin(success = false)
            } finally {
                if (!uiState.successJoin) onJoiningChanged(false)
            }
        }
    }

    override fun onCleared() {
        onJoiningChanged(false)
    }

    companion object {
        private const val VISIT_RECORDED_KEY = "join.visitRecorded"

        fun joinViewModelFactory(
            visit: GroupJoinVisit,
            initialCode: String?,
            groupRepository: GroupRepository,
            crashReporter: CrashReporter,
            analyticsTracker: AnalyticsTracker,
            onJoiningChanged: (Boolean) -> Unit = {},
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                JoinViewModel(
                    visit = visit,
                    initialCode = initialCode,
                    groupRepository = groupRepository,
                    crashReporter = crashReporter,
                    analyticsTracker = analyticsTracker,
                    savedStateHandle = createSavedStateHandle(),
                    onJoiningChanged = onJoiningChanged,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = TrackedScreen.GROUP_JOIN,
        operation = operation,
        entryPoint = visit.entryPoint.value,
    )
}
