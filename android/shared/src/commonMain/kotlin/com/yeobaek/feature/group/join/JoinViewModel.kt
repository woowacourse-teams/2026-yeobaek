package com.yeobaek.feature.group.join

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
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.UserRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class JoinViewModel(
    private val groupRepository: GroupRepository,
    private val crashReporter: CrashReporter,
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.GROUP_JOIN_FAILED),
                )
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
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                JoinViewModel(
                    groupRepository = groupRepository,
                    crashReporter = crashReporter,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = CrashScreen.GROUP_JOIN,
        operation = operation,
    )
}
