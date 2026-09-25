package com.yeobaek.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.yeobaek.feature.onboarding.model.OnboardingBookUiModel

class OnboardingViewModel : ViewModel() {
    var uiState by mutableStateOf(OnboardingUiState())
        private set

    init {
        initList()
    }

    fun initList() {
        uiState = uiState.copy(
            bookUiModelList = mockBookList,
        )
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

    fun dismissDialog() {
        uiState = uiState.copy(
            selectedBookUiState = uiState.selectedBookUiState.copy(
                isSelected = false,
            ),
        )
    }

    companion object {
        val mockBookList = listOf(
            OnboardingBookUiModel(
                id = 0L,
                title = "The Great Gatsby",
                authors = "F. Scott Fitzgerald",
                coverUrl = "https://template.canva.com/EAF8mKE9JzY/1/0/1003w-F9EPCH-Tgf0.jpg",
            ),
            OnboardingBookUiModel(
                id = 1L,
                title = "To Kill a Mockingbird",
                authors = "Harper Lee",
            ),
            OnboardingBookUiModel(
                id = 2L,
                title = "1984",
                authors = "George Orwell",
            ),
            OnboardingBookUiModel(
                id = 3L,
                title = "Pride and Prejudice",
                authors = "Jane Austen",
            ),
            OnboardingBookUiModel(
                id = 4L,
                title = "The Catcher in the Rye",
                authors = "J.D. Salinger",
            ),
        )
    }
}
