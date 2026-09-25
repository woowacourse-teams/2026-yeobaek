package com.yeobaek.feature.onboarding.create.model

import com.yeobaek.data.model.BookModel

data class SelectBookUiModel(
    val id: Long = 0L,
    val title: String = "",
    val authors: String = "",
    val coverUrl: String? = null,
)

fun BookModel.toUiModel() = SelectBookUiModel(
    id = id,
    title = title,
    authors = authors,
    coverUrl = coverImageUrl,
)
