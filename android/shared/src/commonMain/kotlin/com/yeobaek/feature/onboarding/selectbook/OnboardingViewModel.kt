package com.yeobaek.feature.onboarding.selectbook

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
import com.yeobaek.core.analytics.ReadingOnboardingBookSelected
import com.yeobaek.core.analytics.ReadingOnboardingOption
import com.yeobaek.core.analytics.ReadingOnboardingOptionSelected
import com.yeobaek.core.analytics.ReadingOnboardingPublicRoomJoinSubmitted
import com.yeobaek.core.common.TrackedScreen
import com.yeobaek.core.crashlytics.CrashContext
import com.yeobaek.core.crashlytics.CrashLogLevel
import com.yeobaek.core.crashlytics.CrashOperation
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.PublicRoomRepository
import com.yeobaek.feature.book.detail.BookDetailState
import com.yeobaek.feature.onboarding.selectbook.model.toUiModel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import com.yeobaek.feature.book.detail.model.toUiModel as toBookDetailUiModel

class OnboardingViewModel(
    private val bookRepository: BookRepository,
    private val publicRoomRepository: PublicRoomRepository,
    private val crashReporter: CrashReporter,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    var uiState by mutableStateOf(OnboardingUiState())
        private set
    var currentAttemptId: String? = null
        private set
    private var searchJob: Job? = null
    private var bookDetailJob: Job? = null

    init {
        initList()
    }

    fun initList() {
        searchJob?.cancel()
        uiState = uiState.copy(
            initBookState = InitBookState.Loading,
            searchState = SearchState.Idle,
        )

        viewModelScope.launch {
            try {
                val bookList = bookRepository.getBooks()
                crashReporter.track(
                    level = CrashLogLevel.INFO,
                    context = CrashContext(
                        screen = TrackedScreen.ONBOARDING,
                        operation = CrashOperation.ONBOARDING_BOOKS_LOADED,
                        itemCount = bookList.size,
                    ),
                )
                uiState = uiState.copy(
                    bookUiModelList = bookList.map { it.toUiModel() },
                    initBookState = InitBookState.Success,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = CrashContext(
                        screen = TrackedScreen.ONBOARDING,
                        operation = CrashOperation.ONBOARDING_BOOKS_LOAD_FAILED,
                    ),
                )
                uiState = uiState.copy(initBookState = InitBookState.Failure(e.message ?: "알 수 없는 오류"))
            }
        }
    }

    fun updateSearchTextValue(newValue: String) {
        if (uiState.initBookState !is InitBookState.Success) return

        uiState = uiState.copy(
            searchTextValue = newValue,
            searchState = SearchState.Loading,
        )

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                if (newValue.isNotBlank()) delay(300.milliseconds)

                val books = bookRepository.getBooks(newValue)
                currentCoroutineContext().ensureActive()
                uiState = uiState.copy(
                    bookUiModelList = books.map { it.toUiModel() },
                    searchState = SearchState.Success,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(
                    searchState = SearchState.Failure(e.message ?: "책 검색에 실패했습니다."),
                )
            }
        }
    }

    fun onSelectBook(id: Long) {
        if (uiState.publicRoomState is PublicRoomState.Loading) return
        val selectedBook = uiState.bookUiModelList.firstOrNull { it.id == id } ?: return
        currentAttemptId = Random.nextLong().toULong().toString(16)
        analyticsTracker.track(ReadingOnboardingBookSelected(id, requireNotNull(currentAttemptId)))
        uiState = uiState.copy(
            selectedBookUiState = SelectedBookUiState(selectedBook = selectedBook),
            publicRoomState = PublicRoomState.Idle,
        )
        loadBookDetail(id)
    }

    fun retryBookDetail() {
        val bookId = uiState.selectedBookUiState.selectedBook?.id ?: return
        if (uiState.bookDetailState is BookDetailState.Idle) return
        loadBookDetail(bookId)
    }

    private fun loadBookDetail(bookId: Long) {
        bookDetailJob?.cancel()
        uiState = uiState.copy(bookDetailState = BookDetailState.Loading)
        bookDetailJob = viewModelScope.launch {
            try {
                val book = bookRepository.getBookDetail(bookId).toBookDetailUiModel()
                currentCoroutineContext().ensureActive()
                if (uiState.selectedBookUiState.selectedBook?.id == bookId) {
                    uiState = uiState.copy(bookDetailState = BookDetailState.Success(book))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                if (uiState.selectedBookUiState.selectedBook?.id == bookId) {
                    uiState = uiState.copy(
                        bookDetailState = BookDetailState.Failure(e.message ?: "책 정보를 불러오지 못했습니다."),
                    )
                }
            }
        }
    }

    fun showReadingOptions() {
        if (uiState.bookDetailState !is BookDetailState.Success) return
        if (uiState.publicRoomState is PublicRoomState.Loading) return
        uiState = uiState.copy(
            selectedBookUiState = uiState.selectedBookUiState.copy(isSelected = true),
            publicRoomState = PublicRoomState.Idle,
        )
    }

    fun closeBookDetail() {
        if (uiState.publicRoomState is PublicRoomState.Loading) return
        bookDetailJob?.cancel()
        currentAttemptId = null
        uiState = uiState.copy(
            bookDetailState = BookDetailState.Idle,
            selectedBookUiState = SelectedBookUiState(),
            publicRoomState = PublicRoomState.Idle,
        )
    }

    fun closeBottomSheet() {
        uiState = uiState.copy(
            selectedBookUiState = uiState.selectedBookUiState.copy(
                isSelected = false,
            ),
        )
    }

    fun selectGroupCreate(bookId: Long): String? {
        if (uiState.publicRoomState is PublicRoomState.Loading) return null
        val attemptId = currentAttemptId ?: return null
        analyticsTracker.track(
            ReadingOnboardingOptionSelected(bookId, attemptId, ReadingOnboardingOption.GROUP_CREATE),
        )
        return attemptId
    }

    fun onClickJoinPublicRoom(bookId: Long) {
        if (uiState.publicRoomState is PublicRoomState.Loading) return
        val attemptId = currentAttemptId ?: return
        analyticsTracker.track(
            ReadingOnboardingOptionSelected(bookId, attemptId, ReadingOnboardingOption.PUBLIC_ROOM),
        )
        uiState = uiState.copy(publicRoomState = PublicRoomState.Loading)
        crashReporter.track(
            level = CrashLogLevel.INFO,
            context = publicRoomContext(CrashOperation.ONBOARDING_JOIN_STARTED, bookId),
        )
        viewModelScope.launch {
            var publicRoomId: Long? = null
            try {
                val publicRooms = publicRoomRepository.getPublicRooms()
                    .firstOrNull { selectedBook -> selectedBook.book.bookId == bookId }

                if (publicRooms != null) {
                    publicRoomId = publicRooms.publicRoomId
                    publicRoomRepository.visitPublicRoom(publicRooms.publicRoomId)

                    crashReporter.track(
                        level = CrashLogLevel.INFO,
                        context = publicRoomContext(
                            CrashOperation.ONBOARDING_JOIN_SUCCEEDED,
                            bookId,
                            publicRooms.publicRoomId,
                        ),
                    )

                    analyticsTracker.track(
                        ReadingOnboardingPublicRoomJoinSubmitted(
                            bookId,
                            attemptId,
                            EventResult.SUCCESS,
                            publicRooms.publicRoomId,
                        ),
                    )

                    uiState = uiState.copy(
                        publicRoomState = PublicRoomState.Success,
                    )
                } else {
                    crashReporter.track(
                        level = CrashLogLevel.WARN,
                        context = publicRoomContext(CrashOperation.ONBOARDING_PUBLIC_ROOM_NOT_FOUND, bookId),
                    )
                    analyticsTracker.track(
                        ReadingOnboardingPublicRoomJoinSubmitted(bookId, attemptId, EventResult.FAILURE),
                    )
                    uiState = uiState.copy(
                        publicRoomState = PublicRoomState.Failure("해당 책에 대한 공개방이 없습니다."),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                crashReporter.recordException(
                    throwable = e,
                    context = publicRoomContext(CrashOperation.ONBOARDING_JOIN_FAILED, bookId, publicRoomId),
                )
                analyticsTracker.track(
                    ReadingOnboardingPublicRoomJoinSubmitted(bookId, attemptId, EventResult.FAILURE),
                )
                uiState = uiState.copy(publicRoomState = PublicRoomState.Failure(e.message ?: "알 수 없는 오류"))
            }
        }
    }

    fun dismissDialog() {
        uiState = uiState.copy(
            selectedBookUiState = uiState.selectedBookUiState.copy(
                isSelected = false,
            ),
        )
    }

    private fun publicRoomContext(
        operation: CrashOperation,
        bookId: Long,
        publicRoomId: Long? = null,
    ) = CrashContext(
        screen = TrackedScreen.ONBOARDING,
        operation = operation,
        bookId = bookId,
        publicRoomId = publicRoomId,
        readingSpace = "public_room",
    )

    companion object {
        fun onboardingViewModelFactory(
            bookRepository: BookRepository,
            publicRoomRepository: PublicRoomRepository,
            crashReporter: CrashReporter,
            analyticsTracker: AnalyticsTracker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                OnboardingViewModel(
                    bookRepository = bookRepository,
                    publicRoomRepository = publicRoomRepository,
                    crashReporter = crashReporter,
                    analyticsTracker = analyticsTracker,
                )
            }
        }
    }
}

sealed class InitBookState {
    data object Idle : InitBookState()
    data object Loading : InitBookState()
    data object Success : InitBookState()
    data class Failure(val message: String) : InitBookState()
}

sealed class PublicRoomState {
    data object Idle : PublicRoomState()
    data object Loading : PublicRoomState()
    data object Success : PublicRoomState()
    data class Failure(val message: String) : PublicRoomState()
}

sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data object Success : SearchState()
    data class Failure(val message: String) : SearchState()
}
