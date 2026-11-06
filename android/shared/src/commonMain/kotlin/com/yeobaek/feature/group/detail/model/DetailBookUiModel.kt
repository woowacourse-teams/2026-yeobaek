package com.yeobaek.feature.group.detail.model

data class DetailBookUiModel(
    val uri: String? = null,
    val title: String = "",
    val author: List<String> = emptyList(),
    val currentProgress: Int = 0,
    val genres: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
)
