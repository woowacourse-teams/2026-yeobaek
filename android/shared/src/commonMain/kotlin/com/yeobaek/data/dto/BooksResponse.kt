package com.yeobaek.data.dto

import com.yeobaek.data.model.BookModel

data class BooksResponse(
    val books: List<Book>,
)

fun BooksResponse.toModel(): List<BookModel> {
    return books.map { book ->
        book.toModel()
    }
}
