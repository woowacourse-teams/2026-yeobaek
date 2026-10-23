package com.yeobaek.data.repository

import com.yeobaek.data.model.BookDetailModel
import com.yeobaek.data.model.BookModel

interface BookRepository {
    suspend fun getBooks(keyword: String = ""): List<BookModel>
    suspend fun getBook(bookId: Long): BookModel
    suspend fun getBookDetail(bookId: Long): BookDetailModel
}
