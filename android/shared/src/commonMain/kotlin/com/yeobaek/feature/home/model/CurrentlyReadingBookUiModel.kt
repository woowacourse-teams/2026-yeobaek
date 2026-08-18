package com.yeobaek.feature.home.model

data class CurrentlyReadingBookUiModel(
    val clubId: Int,
    val groupName: String,
    val title: String,
    val coverImageUrl: String,
    val authors: String,
    val progressRate: Int,
)
