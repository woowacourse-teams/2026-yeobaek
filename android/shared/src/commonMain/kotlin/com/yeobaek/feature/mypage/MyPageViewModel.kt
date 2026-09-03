package com.yeobaek.feature.mypage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.data.repository.UserRepository
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.launch

class MyPageViewModel(
    private val userRepository: UserRepository,
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
                name = userRepository.getUsername()
            )
        }
    }

    fun deleteAccount() {
        if (uiState.deleteState is DeleteState.Loading) return

        uiState = uiState.copy(
            deleteState = DeleteState.Loading
        )

        viewModelScope.launch {
            try {
                userRepository.deleteAccount()
                uiState = uiState.copy(
                    deleteState = DeleteState.Success
                )
            } catch (e: CancellationException) {
                e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    deleteState = DeleteState.Failure(e.message ?: "알 수 없는 오류가 발생했습니다.")
                )
            }
        }
    }

    companion object {
        fun myPageViewModelFactory(
            userRepository: UserRepository,
        ) : ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MyPageViewModel(
                    userRepository = userRepository,
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
