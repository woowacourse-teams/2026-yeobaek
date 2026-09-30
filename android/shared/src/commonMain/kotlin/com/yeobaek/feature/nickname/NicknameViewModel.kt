package com.yeobaek.feature.nickname

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
import com.yeobaek.core.analytics.InvalidReason
import com.yeobaek.core.analytics.NicknameSubmitted
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.repository.UserRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class NicknameViewModel(
    private val userRepository: UserRepository,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    var uiState by mutableStateOf(NicknameUiState())
        private set

    fun onNicknameValueChange(inputValue: String) {
        uiState = uiState.copy(
            nicknameValue = inputValue,
            nicknameState = true,
            isEnabled = true,
        )
    }

    fun checkNickname() {
        uiState = uiState.copy(
            nicknameState = uiState.nicknameValue.isNotBlank(),
        )
    }

    fun setNickname() {
        checkNickname()
        if (!uiState.nicknameState) {
            analyticsTracker.track(
                NicknameSubmitted(
                    result = EventResult.INVALID,
                    reason = InvalidReason.NICKNAME_BLANK,
                ),
            )
            return
        }

        uiState = uiState.copy(
            isEnabled = false,
        )
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = crashContext(CrashOperation.NICKNAME_SUBMIT_STARTED),
        )

        viewModelScope.launch {
            try {
                userRepository.setUserData(uiState.nicknameValue)
                uiState = uiState.copy(
                    isEnabled = true,
                    successNicknameSet = true,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = crashContext(CrashOperation.NICKNAME_SUBMIT_SUCCEEDED),
                )
                analyticsTracker.track(NicknameSubmitted(result = EventResult.SUCCESS))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.NICKNAME_SUBMIT_FAILED),
                )
                analyticsTracker.track(NicknameSubmitted(result = EventResult.FAILURE))
                uiState = uiState.copy(
                    isEnabled = false,
                    nicknameState = false,
                    successNicknameSet = false,
                )
            }
        }
    }

    companion object {
        fun nicknameViewModelFactory(
            userRepository: UserRepository,
            crashReporter: CrashReporter,
            analyticsTracker: AnalyticsTracker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NicknameViewModel(
                    userRepository = userRepository,
                    crashReporter = crashReporter,
                    analyticsTracker = analyticsTracker,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = TrackedScreen.NICKNAME,
        operation = operation,
    )
}
