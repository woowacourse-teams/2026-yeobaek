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
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.PublicRoomRepository
import com.yeobaek.feature.onboarding.selectbook.model.toUiModel
import io.ktor.utils.io.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val bookRepository: BookRepository,
    private val publicRoomRepository: PublicRoomRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    var uiState by mutableStateOf(OnboardingUiState())
        private set
    var currentAttemptId: String? = null
        private set

    init {
        initList()
    }

    fun initList() {
        uiState = uiState.copy(
            initBookState = InitBookState.Loading,
        )

        viewModelScope.launch {
            try {
                val bookList = bookRepository.getBooks()
                uiState = uiState.copy(
                    bookUiModelList = bookList.map { it.toUiModel() },
                    initBookState = InitBookState.Success,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = uiState.copy(initBookState = InitBookState.Failure(e.message ?: "알 수 없는 오류"))
            }
        }
    }

    fun onSelectBook(id: Long) {
        if (uiState.publicRoomState is PublicRoomState.Loading) return
        val selectedBook = uiState.bookUiModelList.firstOrNull { bookUiModel -> bookUiModel.id == id }
        uiState = if (selectedBook != null) {
            currentAttemptId = Random.nextLong().toULong().toString(16)
            analyticsTracker.track(ReadingOnboardingBookSelected(id, requireNotNull(currentAttemptId)))
            uiState.copy(
                selectedBookUiState = uiState.selectedBookUiState.copy(
                    selectedBook = selectedBook,
                    isSelected = true,
                ),
                publicRoomState = PublicRoomState.Idle,
            )
        } else {
            currentAttemptId = null
            uiState.copy(
                selectedBookUiState = uiState.selectedBookUiState.copy(
                    isSelected = false,
                ),
            )
        }
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
        viewModelScope.launch {
            try {
                val publicRooms = publicRoomRepository.getPublicRooms()
                    .firstOrNull { selectedBook -> selectedBook.book.bookId == bookId }

                if (publicRooms != null) {
                    val publicRoomId = publicRooms.publicRoomId
                    publicRoomRepository.visitPublicRoom(publicRoomId)

                    analyticsTracker.track(
                        ReadingOnboardingPublicRoomJoinSubmitted(
                            bookId,
                            attemptId,
                            EventResult.SUCCESS,
                            publicRoomId,
                        ),
                    )

                    uiState = uiState.copy(
                        publicRoomState = PublicRoomState.Success,
                    )
                } else {
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

    companion object {
        fun onboardingViewModelFactory(
            bookRepository: BookRepository,
            publicRoomRepository: PublicRoomRepository,
            analyticsTracker: AnalyticsTracker,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                OnboardingViewModel(
                    bookRepository = bookRepository,
                    publicRoomRepository = publicRoomRepository,
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
