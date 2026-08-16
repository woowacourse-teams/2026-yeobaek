package com.yeobaek.core.app

import com.russhwolf.settings.Settings
import com.yeobaek.core.network.ApiProvider
import com.yeobaek.core.network.NetworkProvider
import com.yeobaek.data.local.UserPreferences
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.ReaderRepository
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.data.repositoryImpl.remote.BookRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.GroupRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.ReaderRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.UserRepositoryImpl

class AppContainer(
    private val isDebug: Boolean,
) {
    private val networkProvider = NetworkProvider(
        isDebug = isDebug,
    )
    private val settings = Settings()

    val userPreferences = UserPreferences(settings)

    private val apiProvider = ApiProvider(
        ktorfit = networkProvider.ktorfit,
    )

    val userRepository: UserRepository = UserRepositoryImpl(apiProvider.userApi, userPreferences)
    val bookRepository: BookRepository = BookRepositoryImpl(apiProvider.bookApi)
    val groupRepository: GroupRepository = GroupRepositoryImpl(apiProvider.clubApi)
    val readerRepository: ReaderRepository = ReaderRepositoryImpl(apiProvider.readerApi)

    fun close() {
        networkProvider.close()
    }
}
