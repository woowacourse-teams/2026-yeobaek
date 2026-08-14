package com.yeobaek.feature.onboarding

data class OnboardingUiState(
    val username: String = "",
    val userId: Int = 0,
    val codeValue: String = "",
    val codeState: Boolean = false,
    val successJoin: Boolean = false,
)
