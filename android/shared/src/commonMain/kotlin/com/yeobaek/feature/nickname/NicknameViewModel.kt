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
import kotlin.coroutines.cancellation.CancellationException
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
            isEnabled = true,
        )
    }

    fun checkNickname() {
        uiState = uiState.copy(
            nicknameState = uiState.nicknameValue.isBlank(),
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
                        isEnabled = true,
                        )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    uiState = uiState.copy(
                        nicknameState = true,
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
