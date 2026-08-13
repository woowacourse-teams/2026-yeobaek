package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.BookApi
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.repository.BookRepository

class RemoteBookRepositoryImpl(
    private val bookApi: BookApi
) : BookRepository {
    override suspend fun getBooks(
        userId: Int
    ): List<BookModel> {
        return bookApi.getBooks(
            userId = userId
        ).toModel()
    }

    override suspend fun getBook(userId: Int, bookId: Int): BookModel {
        return getBooks(userId).find { it.id == bookId } ?: throw IllegalArgumentException("존재하지 않는 책입니다.")
    }
}
