package com.yeobaek.data.model

data class BookDetailModel(
    val authors: List<String>,
    val bookId: Int,
    val chapters: List<ChapterModel>,
    val passageCount: Int,
    val publishedYear: Int,
    val publisher: String,
    val title: String,
)
