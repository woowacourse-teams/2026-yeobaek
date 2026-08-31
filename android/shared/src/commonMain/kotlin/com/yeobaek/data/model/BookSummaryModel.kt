package com.yeobaek.data.model

data class BookSummaryModel(
    val authors: List<String>,
    val bookId: Long,
    val passageCount: Int,
    val title: String,
    val coverImageUrl: String?,
)
