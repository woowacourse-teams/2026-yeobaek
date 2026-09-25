package com.yeobaek.feature.onboarding.selectbook

import com.yeobaek.feature.onboarding.selectbook.model.OnboardingBookUiModel

data class SelectedBookUiState(
    val selectedBook: OnboardingBookUiModel? = null,
    val isSelected: Boolean = false,
)
