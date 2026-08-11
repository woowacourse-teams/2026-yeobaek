package com.yeobaek.feature.group.create.model

data class CreateBookUiModel(
    val id: Int = 0,
    val uri: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val selected: Boolean = false,
)
