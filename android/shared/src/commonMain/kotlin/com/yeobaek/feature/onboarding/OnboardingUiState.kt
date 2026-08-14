package com.yeobaek.feature.onboarding

import com.yeobaek.feature.ScreenState

data class OnboardingUiState(
    val codeValue: String = "",
    val codeState: Boolean = false,
    val nicknameValue: String = "",
    val nicknameState: Boolean = false,
    val screenState: ScreenState = ScreenState.Onboarding,
)
