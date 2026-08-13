package com.yeobaek.data.dto

import com.yeobaek.data.model.BookModel

data class Book(
    val authors: List<String>,
    val bookId: Int,
    val passageCount: Int,
    val title: String
)

fun Book.toModel(): BookModel {
    return BookModel(
        id = bookId,
        uri = "",
        title = title,
        author = authors.joinToString(),
        progressRate = 0f,
        description = "",
    )
}
