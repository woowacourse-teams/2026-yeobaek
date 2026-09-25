package com.yeobaek.feature.onboarding.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.onboarding.create.model.toUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val groupRepository: GroupRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {
    var uiState by mutableStateOf(CreateGroupUiState())
        private set

    fun initSelectBook(bookId: Long) {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId)
            uiState = uiState.copy(selectBookUiModel = book.toUiModel())
        }
    }

    fun updateGroupNameValue(groupName: String) {
        if (!uiState.isGroupNameValid && checkGroupName()) {
            uiState = uiState.copy(isGroupNameValid = true)
        }
        uiState = uiState.copy(groupName = groupName)
    }

    fun checkGroupName(): Boolean = uiState.groupName.isNotBlank()

    fun createGroup() {
        if (!checkGroupName()) {
            uiState = uiState.copy(
                isGroupNameValid = false,
            )
            return
        }
        if (uiState.createGroupState is CreateGroupState.Loading) return

        uiState = uiState.copy(createGroupState = CreateGroupState.Loading)

        viewModelScope.launch {
            try {
                groupRepository.createGroup(
                    groupName = uiState.groupName,
                    bookId = uiState.selectBookUiModel.id,
                )
                uiState = uiState.copy(createGroupState = CreateGroupState.Success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(createGroupState = CreateGroupState.Failure(e.message ?: "알 수 없는 오류"))
            }
        }
    }

    companion object {
        fun createGroupViewModelFactory(
            groupRepository: GroupRepository,
            bookRepository: BookRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CreateGroupViewModel(
                    groupRepository = groupRepository,
                    bookRepository = bookRepository,
                )
            }
        }
    }
}

sealed class CreateGroupState {
    data object Idle : CreateGroupState()
    data object Loading : CreateGroupState()
    data object Success : CreateGroupState()
    data class Failure(val message: String) : CreateGroupState()
}
