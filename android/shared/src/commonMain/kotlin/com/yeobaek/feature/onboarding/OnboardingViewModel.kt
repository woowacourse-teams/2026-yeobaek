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

    companion object {
        val mockBookList = listOf(
            OnboardingBookUiModel(
                id = 0,
                title = "The Great Gatsby",
                authors = "F. Scott Fitzgerald",
                coverUrl = "https://template.canva.com/EAF8mKE9JzY/1/0/1003w-F9EPCH-Tgf0.jpg",
            ),
            OnboardingBookUiModel(
                id = 1,
                title = "To Kill a Mockingbird",
                authors = "Harper Lee",
                coverUrl = "https://example.com/mockingbird_cover.jpg",
            ),
            OnboardingBookUiModel(
                id = 2,
                title = "1984",
                authors = "George Orwell",
                coverUrl = "https://example.com/1984_cover.jpg",
            ),
            OnboardingBookUiModel(
                id = 3,
                title = "Pride and Prejudice",
                authors = "Jane Austen",
                coverUrl = "https://example.com/pride_prejudice_cover.jpg",
            ),
            OnboardingBookUiModel(
                id = 4,
                title = "The Catcher in the Rye",
                authors = "J.D. Salinger",
                coverUrl = "https://example.com/catcher_in_the_rye_cover.jpg",
            ),
        )
    }
}
