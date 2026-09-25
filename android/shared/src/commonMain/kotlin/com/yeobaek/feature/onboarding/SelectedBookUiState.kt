package com.yeobaek.feature.onboarding

import com.yeobaek.feature.onboarding.model.OnboardingBookUiModel

data class SelectedBookUiState(
    val selectedBook: OnboardingBookUiModel? = null,
    val isSelected: Boolean = false,
)
