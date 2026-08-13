package com.yeobaek.data.repository

import com.yeobaek.data.model.BookDetailModel
import com.yeobaek.data.model.BookModel

interface BookRepository {
    suspend fun getBooks(userId: Int): List<BookModel>
    suspend fun getBook(userId: Int, bookId: Int): BookModel
    suspend fun getBookDetail(bookId: Int): BookDetailModel
}
