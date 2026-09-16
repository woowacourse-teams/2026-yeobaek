package com.yeobaek.feature.group.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.core.analytics.AnalyticsTracker
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.GroupCreateSubmitted
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.group.create.model.CreateBookUiModel
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

class CreateViewModel(
    private val groupRepository: GroupRepository,
    private val bookRepository: BookRepository,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    var uiState by mutableStateOf(CreateUiState())
        private set

    init {
        initBookList()
    }

    fun initBookList() {
        uiState = uiState.copy(
            bookState = BookState.Loading,
        )

        viewModelScope.launch {
            try {
                val groups = bookRepository.getBooks()
                uiState = uiState.copy(
                    bookList = groups.map {
                        CreateBookUiModel(
                            id = it.id,
                            uri = it.coverImageUrl,
                            title = it.title,
                            authors = it.authors,
                            description = it.description,
                        )
                    },
                    bookState = BookState.Success,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = TrackedScreen.GROUP_CREATE,
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
                    bookState = BookState.Failure(e.message ?: "책 목록을 가져오는데 실패했습니다."),
                    bookList = emptyList(),
                )
            }
        }
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
        val selectedBook = uiState.bookList.find { it.selected }
        val selectedBookId = selectedBook?.id

        if (uiState.createState is CreateState.Loading) return

        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = CrashContext(
                screen = TrackedScreen.GROUP_CREATE,
                operation = CrashOperation.GROUP_CREATE_STARTED,
                bookId = selectedBookId,
            ),
        )

        uiState = uiState.copy(
            createState = CreateState.Loading,
        )

        viewModelScope.launch {
            try {
                val bookId = selectedBookId ?: throw IllegalArgumentException("선택된 책이 없습니다.")

                groupRepository.createGroup(
                    groupName = uiState.groupNameValue,
                    bookId = bookId,
                )

                uiState = uiState.copy(
                    createState = CreateState.Success,
                )
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = TrackedScreen.GROUP_CREATE,
                        operation = CrashOperation.GROUP_CREATE_SUCCEEDED,
                        bookId = bookId,
                    ),
                )
                analyticsTracker.track(
                    GroupCreateSubmitted(
                        result = EventResult.SUCCESS,
                        bookId = bookId,
                        bookTitle = selectedBook?.title,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = CrashContext(
                        screen = TrackedScreen.GROUP_CREATE,
                        operation = CrashOperation.GROUP_CREATE_FAILED,
                        bookId = selectedBookId,
                    ),
                )
                analyticsTracker.track(
                    GroupCreateSubmitted(
                        result = EventResult.FAILURE,
                        bookId = selectedBookId,
                        bookTitle = selectedBook?.title,
                    ),
                )
                uiState = uiState.copy(
                    createState = CreateState.Failure(e.message ?: "그룹 생성에 실패했습니다."),
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
            analyticsTracker: AnalyticsTracker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CreateViewModel(
                    groupRepository = groupRepository,
                    bookRepository = bookRepository,
                    crashReporter = crashReporter,
                    analyticsTracker = analyticsTracker,
                )
            }
        }
    }

    private fun crashContext(operation: CrashOperation) = CrashContext(
        screen = TrackedScreen.GROUP_CREATE,
        operation = operation,
    )
}

sealed class BookState {
    data object Idle : BookState()
    data object Loading : BookState()
    data object Success : BookState()
    data class Failure(val message: String) : BookState()
}

sealed class CreateState {
    data object Idle : CreateState()
    data object Loading : CreateState()
    data object Success : CreateState()
    data class Failure(val message: String) : CreateState()
}
