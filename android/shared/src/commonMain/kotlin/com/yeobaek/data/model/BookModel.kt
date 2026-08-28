package com.yeobaek.data.model

data class BookModel(
    val id: Long = 0,
    val coverImageUrl: String? = null,
    val title: String = "",
    val authors: String = "",
    val description: String = "",
    val progressRate: Float = 0f,
)
