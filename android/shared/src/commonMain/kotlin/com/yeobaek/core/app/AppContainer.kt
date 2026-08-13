package com.yeobaek.core.app

import com.yeobaek.core.network.ApiProvider
import com.yeobaek.core.network.NetworkProvider
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repositoryImpl.remote.BookRepositoryImpl

class AppContainer {
    private val networkProvider = NetworkProvider()

    private val apiProvider = ApiProvider(
        ktorfit = networkProvider.ktorfit,
    )

    val bookRepository: BookRepository = BookRepositoryImpl(apiProvider.bookApi)

    fun close() {
        networkProvider.close()
    }
}
