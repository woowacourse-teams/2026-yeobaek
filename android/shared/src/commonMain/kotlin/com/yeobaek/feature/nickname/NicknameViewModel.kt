package com.yeobaek.feature.nickname

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.core.network.crash.CrashContext
import com.yeobaek.core.network.crash.CrashLogLevel
import com.yeobaek.core.network.crash.CrashOperation
import com.yeobaek.core.network.crash.CrashScreen
import com.yeobaek.data.repository.UserRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class NicknameViewModel(
    private val userRepository: UserRepository,
    private val crashReporter: CrashReporter,
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
        if (!uiState.nicknameState) return

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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.NICKNAME_SUBMIT_FAILED),
                )
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
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NicknameViewModel(
                    userRepository = userRepository,
                    crashReporter = crashReporter,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = CrashScreen.NICKNAME,
        operation = operation,
    )
}
