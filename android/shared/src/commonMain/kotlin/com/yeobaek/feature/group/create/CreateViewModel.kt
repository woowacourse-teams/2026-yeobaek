package com.yeobaek.feature.group.create

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
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.feature.group.create.model.CreateBookUiModel
import kotlinx.coroutines.launch

class CreateViewModel(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {
    var uiState by mutableStateOf(CreateUiState())
        private set

    init {
        initBookList()
    }

    fun initBookList() {
        viewModelScope.launch {
            val userId = userRepository.getUserId()

            val groups = bookRepository.getBooks(userId = userId)
            uiState = CreateUiState(
                bookList = groups.map {
                    CreateBookUiModel(
                        id = it.id,
                        uri = it.uri,
                        title = it.title,
                        author = it.author,
                        description = it.description,
                    )
                },
            )
        }
    }

    fun initInputValue() {
        uiState = uiState.copy(
            groupNameValue = "",
            bookList = uiState.bookList.map {
                it.copy(selected = false)
            },
            groupNameCondition = false,
            selectedBookCondition = false,
        )
    }

    fun updateGroupNameValue(value: String) {
        uiState = uiState.copy(
            groupNameValue = value.take(20),
            groupNameCondition = false,
        )
    }

    fun selectBook(index: Int) {
        uiState = uiState.copy(
            bookList = uiState.bookList.mapIndexed { i, book ->
                if (i == index) {
                    book.copy(selected = !book.selected)
                } else {
                    book.copy(selected = false)
                }
            },
            selectedBookCondition = false,
        )
    }

    fun createGroup() {
        viewModelScope.launch {
            val userId = userRepository.getUserId()
            val bookId = uiState.bookList.find { it.selected }?.id ?: throw IllegalArgumentException("선택된 책이 없습니다.")

            groupRepository.createGroup(
                groupName = uiState.groupNameValue,
                userId = userId,
                bookId = bookId,
            )

            uiState = uiState.copy(
                successCreate = true,
            )
        }
    }

    fun groupNameCheck() {
        uiState = uiState.copy(
            groupNameCondition = uiState.groupNameValue.isBlank(),
        )

        if (uiState.groupNameCondition) {
            uiState = uiState.copy(
                groupNameValue = "",
            )
        }
    }

    fun selectedBookCheck() {
        uiState = uiState.copy(
            selectedBookCondition = uiState.bookList.all { !it.selected },
        )
    }

    fun createConditionCheck(): Boolean {
        groupNameCheck()
        selectedBookCheck()
        return uiState.groupNameCondition || uiState.selectedBookCondition
    }

    companion object {
        fun createViewModelFactory(
            groupRepository: GroupRepository,
            bookRepository: BookRepository,
            userRepository: UserRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CreateViewModel(
                    userRepository = userRepository,
                    groupRepository = groupRepository,
                    bookRepository = bookRepository,
                )
            }
        }
    }
}
