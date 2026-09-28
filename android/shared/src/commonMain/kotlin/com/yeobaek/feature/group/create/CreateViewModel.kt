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
import com.yeobaek.core.analytics.BookListExposure
import com.yeobaek.core.analytics.EventResult
import com.yeobaek.core.analytics.GroupCreateBookSelected
import com.yeobaek.core.analytics.GroupCreateSubmitted
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.feature.group.create.model.SelectBookUiModel
import com.yeobaek.feature.group.create.model.toUiModel
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
                    bookList = groups.map { book -> book.toUiModel() },
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
            isGroupNameValid = true,
        )
    }

    // 스크롤마다 이벤트를 보내지 않고, 가장 아래까지 본 위치만 기억했다가 화면을 떠날 때 함께 보낸다.
    private var maxSeenBookPosition = 0

    fun onLastVisibleBookChanged(lastVisibleIndex: Int) {
        maxSeenBookPosition = maxOf(maxSeenBookPosition, lastVisibleIndex + 1)
    }

    fun bookListExposure(): BookListExposure? {
        val bookCount = uiState.bookList.size
        if (bookCount == 0) return null

        return BookListExposure(
            bookCount = bookCount,
            maxSeenBookPosition = maxSeenBookPosition,
        )
    }

    fun selectBook(book: SelectBookUiModel) {
        val targetBook = uiState.bookList.firstOrNull { item -> item.id == book.id } ?: return
        val isAlreadySelected = targetBook.id == uiState.selectedBook?.id

        uiState = uiState.copy(
            selectedBook = targetBook.takeUnless { isAlreadySelected },
        )

        if (!isAlreadySelected) {
            analyticsTracker.track(
                GroupCreateBookSelected(
                    bookId = targetBook.id,
                    bookTitle = targetBook.title,
                ),
            )
        }
    }

    fun moveToGroupName() {
        if (uiState.bookState !is BookState.Success || uiState.selectedBook == null) return
        uiState = uiState.copy(step = CreateStep.GroupName)
    }

    fun moveToBookSelection() {
        uiState = uiState.copy(step = CreateStep.BookSelection)
    }

    fun createGroup() {
        val selectedBook = uiState.selectedBook
        val selectedBookId = selectedBook?.id
        val groupName = uiState.groupNameValue

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
                    groupName = groupName,
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
                        bookTitle = selectedBook.title,
                        bookList = bookListExposure(),
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
                        bookList = bookListExposure(),
                    ),
                )
                uiState = uiState.copy(
                    createState = CreateState.Failure(e.message ?: "그룹 생성에 실패했습니다."),
                )
            }
        }
    }

    fun groupNameCheck() {
        val isGroupNameValid = uiState.groupNameValue.isNotBlank()
        uiState = uiState.copy(
            groupNameValue = if (isGroupNameValid) uiState.groupNameValue else "",
            isGroupNameValid = isGroupNameValid,
        )
    }

    fun createConditionCheck(): Boolean {
        groupNameCheck()
        return !uiState.isGroupNameValid || uiState.selectedBook == null
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
