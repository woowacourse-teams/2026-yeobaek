package com.yeobaek.feature.onboarding

import com.yeobaek.feature.ScreenState

data class OnboardingUiState(
    val username: String = "",
    val codeValue: String = "",
    val codeState: Boolean = false,
    val screenState: ScreenState = ScreenState.Onboarding,
)
