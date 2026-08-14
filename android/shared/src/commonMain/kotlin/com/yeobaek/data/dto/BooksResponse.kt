package com.yeobaek.data.dto

import com.yeobaek.data.model.BookModel
import kotlinx.serialization.Serializable

@Serializable
data class BooksResponse(
    val books: List<Book>,
)

fun BooksResponse.toModel(): List<BookModel> = books.map { book ->
    book.toModel()
}
