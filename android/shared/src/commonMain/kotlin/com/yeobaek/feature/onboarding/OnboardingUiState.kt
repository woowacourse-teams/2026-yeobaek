package com.yeobaek.feature.onboarding

data class OnboardingUiState(
    val codeValue: String = "",
    val codeState: Boolean = false,
    val nicknameValue: String = "",
    val nicknameState: Boolean = false,
    val setUser: Boolean = false
)
