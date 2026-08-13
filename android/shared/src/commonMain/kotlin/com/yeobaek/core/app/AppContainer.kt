package com.yeobaek.core.app

import com.yeobaek.core.network.ApiProvider
import com.yeobaek.core.network.NetworkProvider
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repositoryImpl.remote.BookRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.GroupRepositoryImpl

class AppContainer(
    val memberId: Int,
) {
    private val networkProvider = NetworkProvider(
        memberId = memberId,
    )

    private val apiProvider = ApiProvider(
        ktorfit = networkProvider.ktorfit,
    )

    val bookRepository: BookRepository = BookRepositoryImpl(apiProvider.bookApi)
    val groupRepository: GroupRepository = GroupRepositoryImpl(apiProvider.clubApi)

    fun close() {
        networkProvider.close()
    }
}
