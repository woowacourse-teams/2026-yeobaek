package com.yeobaek.feature.nickname

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.data.local.UserPreferences
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.feature.ScreenState
import kotlinx.coroutines.launch

class NicknameViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    var uiState by mutableStateOf(NicknameUiState())
        private set

    fun onNicknameValueChange(inputValue: String) {
        uiState = uiState.copy(
            nicknameState = false,
            nicknameValue = inputValue,
        )
    }

    fun setNickname(screenState: ScreenState) {
        viewModelScope.launch {
            try {
                userRepository.setUserData(uiState.nicknameValue)
                uiState = uiState.copy(
                    screenState = screenState
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    nicknameState = true,
                )
            }
        }
    }

    companion object {
        fun nicknameViewModelFactory(
            userRepository: UserRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NicknameViewModel(userRepository)
            }
        }
    }
}
