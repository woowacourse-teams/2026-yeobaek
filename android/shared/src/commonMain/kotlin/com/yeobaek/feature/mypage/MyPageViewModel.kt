package com.yeobaek.feature.mypage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.analytics.AccountDeleted
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.local.GuideOnboardingPreferences
import com.yeobaek.data.repository.UserRepository
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.launch

class MyPageViewModel(
    private val clearGuideState: () -> Unit,
    private val userRepository: UserRepository,
    private val recordException: (Throwable, CrashContext) -> Unit,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    var uiState by mutableStateOf(MyPageUiState())
        private set

    init {
        initMyPage()
    }

    fun initMyPage() {
        viewModelScope.launch {
            uiState = uiState.copy(
                id = userRepository.getUserId(),
                name = userRepository.getUsername(),
            )
        }
    }

    fun deleteAccount() {
        if (uiState.deleteState is DeleteState.Loading || uiState.deleteState is DeleteState.Success) return

        uiState = uiState.copy(
            deleteState = DeleteState.Loading,
        )

        viewModelScope.launch {
            try {
                userRepository.deleteAccount()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                recordException(
                    e,
                    CrashContext(
                        screen = TrackedScreen.MY_PAGE,
                        operation = CrashOperation.ACCOUNT_DELETE_FAILED,
                    ),
                )
                analyticsTracker.track(AccountDeleted(result = EventResult.FAILURE))
                uiState = uiState.copy(
                    deleteState = DeleteState.Failure(e.message ?: "알 수 없는 오류가 발생했습니다."),
                )
                return@launch
            }

            try {
                try {
                    analyticsTracker.track(AccountDeleted(result = EventResult.SUCCESS))
                } finally {
                    analyticsTracker.resetIdentity()
                }
            } catch (e: Exception) {
                recordException(e, cleanupContext())
            }
            try {
                clearGuideState()
            } catch (e: Exception) {
                recordException(e, cleanupContext())
            }
            uiState = uiState.copy(deleteState = DeleteState.Success)
        }
    }

    private fun cleanupContext() = CrashContext(
        screen = TrackedScreen.MY_PAGE,
        operation = CrashOperation.ACCOUNT_DELETE_CLEANUP_FAILED,
    )

    companion object {
        fun myPageViewModelFactory(
            guideOnboardingPreferences: GuideOnboardingPreferences,
            userRepository: UserRepository,
            crashReporter: CrashReporter,
            analyticsTracker: AnalyticsTracker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MyPageViewModel(
                    clearGuideState = guideOnboardingPreferences::clearGuideState,
                    userRepository = userRepository,
                    recordException = crashReporter::recordException,
                    analyticsTracker = analyticsTracker,
                )
            }
        }
    }
}

sealed class DeleteState {
    data object Idle : DeleteState()
    data object Loading : DeleteState()
    data object Success : DeleteState()
    data class Failure(val message: String) : DeleteState()
}
