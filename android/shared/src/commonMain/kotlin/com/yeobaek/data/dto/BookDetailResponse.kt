package com.yeobaek.data.dto

import com.yeobaek.data.model.BookDetailModel
import kotlinx.serialization.Serializable

@Serializable
data class BookDetailResponse(
    val authors: List<String>,
    val bookId: Long,
    val chapters: List<Chapter>,
    val passageCount: Int,
    val publishedYear: Int,
    val publisher: String,
    val title: String,
    val coverImageUrl: String?,
)

fun BookDetailResponse.toModel(): BookDetailModel =
    BookDetailModel(
        authors = authors,
        bookId = bookId,
        chapters = chapters.map { it.toModel() },
        passageCount = passageCount,
        publishedYear = publishedYear,
        publisher = publisher,
        title = title,
        coverImageUrl = coverImageUrl,
    )
