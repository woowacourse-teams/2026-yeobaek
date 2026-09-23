package com.yeobaek.feature.onboarding

import com.yeobaek.feature.onboarding.model.OnboardingBookUiModel

data class OnboardingUiState(
    val bookUiModelList: List<OnboardingBookUiModel> = emptyList(),
)
