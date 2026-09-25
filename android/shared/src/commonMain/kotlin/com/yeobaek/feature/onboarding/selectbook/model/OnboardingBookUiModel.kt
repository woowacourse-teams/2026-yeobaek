package com.yeobaek.feature.onboarding.selectbook.model

data class OnboardingBookUiModel(
    val id: Long = 0L,
    val title: String = "",
    val authors: String = "",
    val coverUrl: String? = null,
)
