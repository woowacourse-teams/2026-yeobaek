package com.yeobaek.data.repositoryImpl

import com.yeobaek.data.MockData
import com.yeobaek.data.model.BookDetailModel
import com.yeobaek.data.model.BookModel
import com.yeobaek.data.repository.BookRepository

class MockBookRepositoryImpl : BookRepository {
    override fun getBooks(): List<BookModel> = MockData.mockBooks

    override fun getBook(id: Int): BookModel = MockData.mockBooks.find {
        it.id == id
    } ?: throw IllegalArgumentException("존재하지 않는 책입니다.")

    override suspend fun getBookDetail(id: Int): BookDetailModel {
        TODO("Not yet implemented")
    }
}
