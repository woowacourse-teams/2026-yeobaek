package com.yeobaek.feature.group.join

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

class JoinViewModel(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
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

    fun joinGroup() {
        viewModelScope.launch {
            val user = userRepository.userModel

            groupRepository.joinGroup(
                joinCode = uiState.codeValue,
                userId = user.id,
            )
        }
    }

    companion object {
        fun joinViewModelFactory(
            userRepository: UserRepository,
            groupRepository: GroupRepository
        ) : ViewModelProvider.Factory = viewModelFactory {
            initializer {
                JoinViewModel(
                    userRepository = userRepository,
                    groupRepository = groupRepository,
                )
            }
        }
    }
}
