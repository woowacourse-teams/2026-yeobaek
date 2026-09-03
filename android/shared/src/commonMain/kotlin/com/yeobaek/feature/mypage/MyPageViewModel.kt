package com.yeobaek.feature.mypage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.data.repository.UserRepository

class MyPageViewModel(
    private val userRepository: UserRepository,
) : ViewModel() {
    var uiState by mutableStateOf(MyPageUiState())
        private set

    fun initMyPage() {
        uiState = uiState.copy(
            id = userRepository.getUserId(),
            name = userRepository.getUsername()
        )
    }

    fun deleteAccount() {
        TODO()
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
