package com.yeobaek.core.app

import com.russhwolf.settings.Settings
import com.yeobaek.core.network.NetworkProvider
import com.yeobaek.data.local.UserPreferences

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

    val bookRepository: BookRepository = BookRepositoryImpl(apiProvider.bookApi)
    val groupRepository: GroupRepository = GroupRepositoryImpl(apiProvider.clubApi)
    val readerRepository: ReaderRepository = ReaderRepositoryImpl(apiProvider.readerApi)

    fun close() {
        networkProvider.close()
    }
}
