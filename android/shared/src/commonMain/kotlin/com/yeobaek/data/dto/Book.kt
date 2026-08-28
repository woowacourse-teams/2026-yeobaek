package com.yeobaek.data.dto

import com.yeobaek.data.model.BookModel
import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val authors: List<String>,
    val bookId: Long,
    val passageCount: Int,
    val title: String,
    val coverImageUrl: String?,
)

fun Book.toModel(): BookModel = BookModel(
    id = bookId,
    coverImageUrl = coverImageUrl,
    title = title,
    authors = authors.joinToString(),
    progressRate = 0f,
    description = "",
)
