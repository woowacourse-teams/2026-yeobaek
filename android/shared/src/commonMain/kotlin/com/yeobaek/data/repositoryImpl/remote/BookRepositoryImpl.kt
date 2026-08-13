package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.BookApi
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.BookDetailModel
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.repository.BookRepository

class BookRepositoryImpl(
    private val bookApi: BookApi,
) : BookRepository {
    override fun getBooks(): List<BookModel> {
        TODO("Not yet implemented")
    }

    override fun getBook(id: Int): BookModel {
        TODO("Not yet implemented")
    }

    override suspend fun getBookDetail(id: Int): BookDetailModel = bookApi
        .getBookDetail(bookId = id)
        .toModel()
}
