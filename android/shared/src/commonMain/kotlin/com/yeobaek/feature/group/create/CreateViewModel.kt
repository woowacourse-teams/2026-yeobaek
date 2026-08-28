package com.yeobaek.feature.group.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.core.network.crash.CrashContext
import com.yeobaek.core.network.crash.CrashLogLevel
import com.yeobaek.core.network.crash.CrashOperation
import com.yeobaek.core.network.crash.CrashScreen
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.group.create.model.CreateBookUiModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class CreateViewModel(
    private val groupRepository: GroupRepository,
    private val bookRepository: BookRepository,
    private val crashReporter: CrashReporter,
) : ViewModel() {
    var uiState by mutableStateOf(CreateUiState())
        private set

    fun initBookList() {
        viewModelScope.launch {
            try {
                val groups = bookRepository.getBooks()
                uiState = uiState.copy(
                    bookList = groups.map {
                        CreateBookUiModel(
                            id = it.id,
                            uri = it.uri,
                            title = it.title,
                            authors = it.authors,
                            description = it.description,
                        )
                    },
                    successBookLoading = true,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = CrashScreen.GROUP_CREATE,
                        operation = CrashOperation.GROUP_BOOKS_LOADED,
                        itemCount = groups.size,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = crashContext(CrashOperation.GROUP_BOOKS_FAILED),
                )
                uiState = uiState.copy(
                    successBookLoading = false,
                    bookList = emptyList(),
                )
            }
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
        val selectedBookId = uiState.bookList.find { it.selected }?.id
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = CrashContext(
                screen = CrashScreen.GROUP_CREATE,
                operation = CrashOperation.GROUP_CREATE_STARTED,
                bookId = selectedBookId,
            ),
        )
        viewModelScope.launch {
            try {
                val bookId = selectedBookId ?: throw IllegalArgumentException("선택된 책이 없습니다.")

                groupRepository.createGroup(
                    groupName = uiState.groupNameValue,
                    bookId = bookId,
                )

                uiState = uiState.copy(
                    successCreate = true,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = CrashScreen.GROUP_CREATE,
                        operation = CrashOperation.GROUP_CREATE_SUCCEEDED,
                        bookId = bookId,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = CrashContext(
                        screen = CrashScreen.GROUP_CREATE,
                        operation = CrashOperation.GROUP_CREATE_FAILED,
                        bookId = selectedBookId,
                    ),
                )
                uiState = uiState.copy(
                    successCreate = false,
                )
            }
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
            crashReporter: CrashReporter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CreateViewModel(
                    groupRepository = groupRepository,
                    bookRepository = bookRepository,
                    crashReporter = crashReporter,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = CrashScreen.GROUP_CREATE,
        operation = operation,
    )
}
