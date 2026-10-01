package com.yeobaek.feature.onboarding.selectbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.PublicRoomRepository
import com.yeobaek.feature.onboarding.selectbook.model.toUiModel
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val bookRepository: BookRepository,
    private val publicRoomRepository: PublicRoomRepository,
) : ViewModel() {
    var uiState by mutableStateOf(OnboardingUiState())
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
        val selectedBook = uiState.bookUiModelList.firstOrNull { bookUiModel -> bookUiModel.id == id }
        uiState = if (selectedBook != null) {
            uiState.copy(
                selectedBookUiState = uiState.selectedBookUiState.copy(
                    selectedBook = selectedBook,
                    isSelected = true,
                ),
            )
        } else {
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

    fun onClickJoinPublicRoom(bookId: Long) {
        viewModelScope.launch {
            try {
                val publicRooms = publicRoomRepository.getPublicRooms()
                    .firstOrNull { selectedBook -> selectedBook.book.bookId == bookId }
                uiState = uiState.copy(
                    publicRoomState = PublicRoomState.Loading,
                )

                if (publicRooms != null) {
                    val publicRoomId = publicRooms.publicRoomId
                    publicRoomRepository.visitPublicRoom(publicRoomId)

                    uiState = uiState.copy(
                        publicRoomState = PublicRoomState.Success,
                    )
                } else {
                    uiState = uiState.copy(
                        publicRoomState = PublicRoomState.Failure("해당 책에 대한 공개방이 없습니다."),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
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
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                OnboardingViewModel(
                    bookRepository = bookRepository,
                    publicRoomRepository = publicRoomRepository,
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
