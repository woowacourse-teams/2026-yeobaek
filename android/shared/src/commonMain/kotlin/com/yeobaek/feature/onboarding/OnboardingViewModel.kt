package com.yeobaek.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.UserRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    var uiState by mutableStateOf(OnboardingUiState())
        private set

    init {
        uiState = uiState.copy(
            username = userRepository.getUsername(),
            userId = userRepository.getUserId(),
        )
    }

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
        viewModelScope.launch {
            try {
                val userId = userRepository.getUserId()

                groupRepository.joinGroup(
                    joinCode = uiState.codeValue,
                    userId = userId,
                )

                uiState = uiState.copy(
                    successJoin = true,
                    codeState = false,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    codeState = true,
                )
            }
        }
    }

    companion object {
        fun onboardingViewModelFactory(
            userRepository: UserRepository,
            groupRepository: GroupRepository,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    OnboardingViewModel(
                        userRepository = userRepository,
                        groupRepository = groupRepository,
                    )
                }
            }
    }
}
