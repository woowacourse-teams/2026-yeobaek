package com.yeobaek.data.model

data class BookSummaryModel(
    val authors: List<String>,
    val bookId: Int,
    val passageCount: Int,
    val title: String,
)
