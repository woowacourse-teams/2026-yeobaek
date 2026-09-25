package com.yeobaek.feature.onboarding.selectbook.model

import com.yeobaek.data.model.BookModel

data class OnboardingBookUiModel(
    val id: Long = 0L,
    val title: String = "",
    val authors: String = "",
    val coverUrl: String? = null,
)

fun BookModel.toUiModel(): OnboardingBookUiModel = OnboardingBookUiModel(
    id = id,
    title = title,
    authors = authors,
    coverUrl = coverImageUrl,
)
