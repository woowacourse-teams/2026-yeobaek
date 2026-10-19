package com.yeobaek.data.model

data class SearchBookModel(
    val authors: List<String>,
    val bookId: Long,
    val coverImageUrl: String,
    val passageCount: Int,
    val publishedYear: Int,
    val publisher: String,
    val title: String
)
