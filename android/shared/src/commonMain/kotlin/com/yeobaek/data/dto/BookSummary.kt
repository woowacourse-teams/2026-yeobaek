package com.yeobaek.data.dto

import com.yeobaek.data.model.BookSummaryModel
import kotlinx.serialization.Serializable

@Serializable
data class BookSummary(
    val authors: List<String>,
    val bookId: Int,
    val passageCount: Int,
    val title: String,
)

fun BookSummary.toModel(): BookSummaryModel =
    BookSummaryModel(
        authors = authors,
        bookId = bookId,
        passageCount = passageCount,
        title = title,
    )
