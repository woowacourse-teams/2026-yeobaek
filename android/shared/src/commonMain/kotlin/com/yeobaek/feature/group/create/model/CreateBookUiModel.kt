package com.yeobaek.feature.group.create.model

data class CreateBookUiModel(
    val id: Long = 0,
    val uri: String?,
    val title: String = "",
    val authors: String = "",
    val description: String = "",
    val selected: Boolean = false,
)
