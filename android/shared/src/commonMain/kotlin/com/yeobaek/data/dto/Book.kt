package com.yeobaek.data.dto

import com.yeobaek.data.model.BookModel
import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val authors: List<String>,
    val bookId: Long,
    val passageCount: Int,
    val title: String,
)

fun Book.toModel(): BookModel = BookModel(
    id = bookId,
    uri = "",
    title = title,
    authors = authors.joinToString(),
    progressRate = 0f,
    description = "",
)
