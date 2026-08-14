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
import com.yeobaek.feature.ScreenState
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
            userId = userRepository.getUserId()
        )
    }

    fun onCodeValueChange(inputValue: String) {
        uiState = uiState.copy(
            codeState = false,
            codeValue = inputValue,
        )
    }

    fun joinGroup(screenState: ScreenState) {
        viewModelScope.launch {
            try {
                val userId = userRepository.getUserId()

                groupRepository.joinGroup(
                    joinCode = uiState.codeValue,
                    userId = userId,
                )

                uiState = uiState.copy(
                    screenState = screenState,
                )
            } catch(e: Exception) {
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
