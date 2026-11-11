package com.yeobaek.data.model

data class BookSummaryModel(
    val authors: List<String>,
    val bookId: Long,
    val passageCount: Int,
    val title: String,
    val coverImageUrl: String?,
    val genres: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
)
