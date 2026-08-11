package com.yeobaek.data.repository

import com.yeobaek.data.model.BookModel

interface BookRepository {
    fun getBooks(): List<BookModel>
    fun getBook(id: Int): BookModel
}
