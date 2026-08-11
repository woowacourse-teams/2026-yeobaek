package com.yeobaek.data.model

data class BookModel(
    val id: Int = 0,
    val uri: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val progressRate: Float = 0f,
)
