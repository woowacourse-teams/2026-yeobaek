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
import com.yeobaek.core.analytics.GroupJoinEntryPoint
import com.yeobaek.core.analytics.GroupJoinSubmitted
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.repository.GroupRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class JoinViewModel(
    private val entryPoint: GroupJoinEntryPoint,
    private val groupRepository: GroupRepository,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val input = JoinInputState(savedStateHandle)
    val uiState: JoinUiState get() = input.value

    fun initializeInput(initialCode: String?, requestId: String?) = input.initialize(initialCode, requestId)

    fun onCodeValueChange(value: String) = input.edit(value)

    fun checkCodeBlank() = input.checkBlank()

    fun joinGroup() {
        val code = input.beginJoin() ?: return
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
                analyticsTracker.track(GroupJoinSubmitted(result = EventResult.SUCCESS, entryPoint = entryPoint))
            } catch (e: CancellationException) {
                input.cancelJoin()
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.GROUP_JOIN_FAILED),
                )
                analyticsTracker.track(GroupJoinSubmitted(result = EventResult.FAILURE, entryPoint = entryPoint))
                input.finishJoin(success = false)
            }
        }
    }

    companion object {
        fun joinViewModelFactory(
            entryPoint: GroupJoinEntryPoint,
            groupRepository: GroupRepository,
            crashReporter: CrashReporter,
            analyticsTracker: AnalyticsTracker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                JoinViewModel(
                    entryPoint = entryPoint,
                    groupRepository = groupRepository,
                    crashReporter = crashReporter,
                    analyticsTracker = analyticsTracker,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = TrackedScreen.GROUP_JOIN,
        operation = operation,
        entryPoint = entryPoint.value,
    )
}
