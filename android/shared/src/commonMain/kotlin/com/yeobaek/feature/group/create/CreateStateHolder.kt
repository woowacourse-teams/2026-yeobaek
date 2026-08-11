package com.yeobaek.feature.group.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.feature.group.create.model.CreateBookUiModel

class CreateStateHolder(
    private val groupRepository: GroupRepository,
    private val bookRepository: BookRepository,
    private val userRepository: UserRepository,
) {
    var uiState by mutableStateOf(CreateUiState())
        private set

    var groupNameCondition by mutableStateOf(false)
    var selectedBookCondition by mutableStateOf(false)

    init {
        initBookList()
    }

    fun initBookList() {
        val groups = bookRepository.getBooks()
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

    fun initInputValue() {
        uiState = uiState.copy(
            groupNameValue = "",
            bookList = uiState.bookList.map {
                it.copy(selected = false)
            },
        )
        groupNameCondition = false
        selectedBookCondition = false
    }

    fun updateGroupNameValue(value: String) {
        uiState = uiState.copy(
            groupNameValue = value.take(20),
        )
        groupNameCondition = false
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
        )
        selectedBookCondition = false
    }

    fun createGroup() {
        val bookId = uiState.bookList.find { it.selected }?.id ?: throw IllegalArgumentException("선택된 책이 없습니다.")
        val selectedBook = bookRepository.getBook(bookId)

        groupRepository.createGroup(
            groupName = uiState.groupNameValue,
            username = userRepository.nickname,
            book = selectedBook,
        )
    }

    fun groupNameCheck() {
        groupNameCondition = uiState.groupNameValue.isBlank()
        if (groupNameCondition) {
            uiState = uiState.copy(
                groupNameValue = "",
            )
        }
    }

    fun selectedBookCheck() {
        selectedBookCondition = uiState.bookList.all { !it.selected }
    }

    fun createConditionCheck(): Boolean {
        groupNameCheck()
        selectedBookCheck()
        return groupNameCondition || selectedBookCondition
    }
}
