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
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    var uiState by mutableStateOf(OnboardingUiState())
        private set

    init {
        uiState = uiState.copy(
            username = userRepository.getUsername()
        )
    }

    fun onCodeValueChange(inputValue: String) {
        uiState = uiState.copy(
            codeState = false,
            codeValue = inputValue,
        )
    }

    fun joinGroup() {
        viewModelScope.launch {
            val userId = userRepository.getUserId()

            groupRepository.joinGroup(
                joinCode = uiState.codeValue,
                userId = userId,
            )
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
