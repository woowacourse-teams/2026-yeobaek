package com.yeobaek.feature.onboarding.selectbook

import com.yeobaek.feature.onboarding.selectbook.model.OnboardingBookUiModel

data class OnboardingUiState(
    val selectedBookUiState: SelectedBookUiState = SelectedBookUiState(),
    val bookUiModelList: List<OnboardingBookUiModel> = emptyList(),
)
