package com.yeobaek.data.repositoryImpl.remote

import com.yeobaek.data.api.BookApi
import com.yeobaek.data.dto.toModel
import com.yeobaek.data.model.BookDetailModel
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.repository.BookRepository

class BookRepositoryImpl(
    private val bookApi: BookApi,
) : BookRepository {
    override suspend fun getBooks(
    ): List<BookModel> {
        val response = bookApi.getBooks()
        return if (response.isSuccessful) {
            response.body()?.toModel() ?: throw IllegalArgumentException("책 정보가 없네요")
        } else {
            throw IllegalArgumentException("책 정보를 가져오는데 실패했습니다 ${response.status}")
        }
    }

    override suspend fun getBook(bookId: Int): BookModel = getBooks().find {
        it.id == bookId
    } ?: throw IllegalArgumentException("존재하지 않는 책입니다.")

    override suspend fun getBookDetail(bookId: Int): BookDetailModel = bookApi
        .getBookDetail(bookId = bookId)
        .toModel()
}
