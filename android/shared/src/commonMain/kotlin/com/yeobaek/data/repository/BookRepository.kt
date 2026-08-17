package com.yeobaek.data.repository

import com.yeobaek.data.model.BookDetailModel
import com.yeobaek.data.model.BookModel

interface BookRepository {
    suspend fun getBooks(): List<BookModel>
    suspend fun getBook(bookId: Int): BookModel
    suspend fun getBookDetail(bookId: Int): BookDetailModel
}
