package com.yeobaek.feature.group.join

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.GroupJoinSubmitted
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.UserRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class JoinViewModel(
    private val groupRepository: GroupRepository,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    var uiState by mutableStateOf(JoinUiState())
        private set

    fun initInputValue() {
        uiState = uiState.copy(
            codeValue = "",
            codeState = false,
        )
    }

    fun onCodeValueChange(value: String) {
        uiState = uiState.copy(
            codeValue = value,
            codeState = false,
        )
    }

    fun checkCodeBlank() {
        uiState = uiState.copy(
            codeState = uiState.codeValue.isBlank(),
        )
    }

    fun joinGroup() {
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = crashContext(CrashOperation.GROUP_JOIN_STARTED),
        )
        viewModelScope.launch {
            try {
                groupRepository.joinGroup(
                    joinCode = uiState.codeValue,
                )

                uiState = uiState.copy(
                    successJoin = true,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = crashContext(CrashOperation.GROUP_JOIN_SUCCEEDED),
                )
                analyticsTracker.track(GroupJoinSubmitted(result = EventResult.SUCCESS))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.GROUP_JOIN_FAILED),
                )
                analyticsTracker.track(GroupJoinSubmitted(result = EventResult.FAILURE))
                uiState = uiState.copy(
                    successJoin = false,
                    codeState = true,
                )
            }
        }
    }

    companion object {
        fun joinViewModelFactory(
            userRepository: UserRepository,
            groupRepository: GroupRepository,
            crashReporter: CrashReporter,
            analyticsTracker: AnalyticsTracker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                JoinViewModel(
                    groupRepository = groupRepository,
                    crashReporter = crashReporter,
                    analyticsTracker = analyticsTracker,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = TrackedScreen.GROUP_JOIN,
        operation = operation,
    )
}
