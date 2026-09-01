package com.yeobaek.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.repository.GroupRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val groupRepository: GroupRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {
    var uiState by mutableStateOf(OnboardingUiState())
        private set

    fun onCodeValueChange(inputValue: String) {
        uiState = uiState.copy(
            codeState = false,
            codeValue = inputValue,
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
            context = crashContext(CrashOperation.ONBOARDING_JOIN_STARTED),
        )
        viewModelScope.launch {
            try {
                groupRepository.joinGroup(
                    joinCode = uiState.codeValue,
                )

                uiState = uiState.copy(
                    successJoin = true,
                    codeState = false,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = crashContext(CrashOperation.ONBOARDING_JOIN_SUCCEEDED),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.ONBOARDING_JOIN_FAILED),
                )
                uiState = uiState.copy(
                    codeState = true,
                )
            }
        }
    }

    companion object {
        fun onboardingViewModelFactory(
            groupRepository: GroupRepository,
            crashReporter: CrashReporter,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    OnboardingViewModel(
                        groupRepository = groupRepository,
                        crashReporter = crashReporter,
                    )
                }
            }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = TrackedScreen.ONBOARDING,
        operation = operation,
    )
}
