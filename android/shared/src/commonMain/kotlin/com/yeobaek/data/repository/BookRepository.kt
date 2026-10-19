package com.yeobaek.data.repository

import com.yeobaek.data.model.BookDetailModel
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.model.SearchBooksModel

interface BookRepository {
    suspend fun getBooks(): List<BookModel>
    suspend fun getBook(bookId: Long): BookModel
    suspend fun getBookDetail(bookId: Long): BookDetailModel
    suspend fun getSearchBooks(keyword: String): SearchBooksModel
}
