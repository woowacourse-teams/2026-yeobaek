package com.yeobaek.data.model

data class BookDetailModel(
    val authors: List<String>,
    val bookId: Long,
    val chapters: List<ChapterModel>,
    val passageCount: Int,
    val title: String,
    val coverImageUrl: String?,
    val description: String?,
    val pageCount: Long?,
)
