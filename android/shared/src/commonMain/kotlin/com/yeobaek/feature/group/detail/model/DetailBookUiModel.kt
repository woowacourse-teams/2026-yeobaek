package com.yeobaek.feature.group.detail.model

data class DetailBookUiModel(
    val uri: String? = null,
    val title: String = "",
    val author: List<String> = emptyList(),
    val currentProgress: Int = 0,
)
