package com.yeobaek.feature.nickname

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.data.repository.UserRepository
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

    fun checkNickname() {
        uiState = uiState.copy(
            nicknameState = uiState.nicknameValue.isBlank()
        )
    }

    fun setNickname() {
        viewModelScope.launch {
            uiState = uiState.copy(
                isEnabled = false,
            )
            if (!uiState.isEnabled) {
                try {
                    userRepository.setUserData(uiState.nicknameValue)
                    uiState = uiState.copy(
                        successNicknameSet = true,
                    )
                } catch (e: Exception) {
                    uiState = uiState.copy(
                        nicknameState = true,
                    )
                } finally {
                    uiState = uiState.copy(
                        isEnabled = true,
                    )
                }
            }
        }
    }

    companion object {
        fun nicknameViewModelFactory(
            userRepository: UserRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NicknameViewModel(userRepository)
            }
        }
    }
}
