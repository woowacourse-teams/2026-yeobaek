package com.yeobaek.feature.onboarding.selectbook

import com.yeobaek.feature.onboarding.selectbook.model.OnboardingBookUiModel

data class OnboardingUiState(
    val selectedBookUiState: SelectedBookUiState = SelectedBookUiState(),
    val searchTextValue: String = "",
    val bookUiModelList: List<OnboardingBookUiModel> = emptyList(),
    val initBookState: InitBookState = InitBookState.Idle,
    val searchState: SearchState = SearchState.Idle,
    val publicRoomState: PublicRoomState = PublicRoomState.Idle,
)
