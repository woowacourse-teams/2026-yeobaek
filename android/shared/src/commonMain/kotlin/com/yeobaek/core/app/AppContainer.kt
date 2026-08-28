package com.yeobaek.core.app

import com.russhwolf.settings.Settings
import com.yeobaek.core.network.ApiProvider
import com.yeobaek.core.network.CrashReporter
import com.yeobaek.core.network.NetworkProvider
import com.yeobaek.data.local.UserPreferences
import com.yeobaek.data.repository.BookRepository
import com.yeobaek.data.repository.CommentRepository
import com.yeobaek.data.repository.GroupRepository
import com.yeobaek.data.repository.ReaderRepository
import com.yeobaek.data.repository.UserRepository
import com.yeobaek.data.repositoryImpl.remote.BookRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.CommentRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.GroupRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.ReaderRepositoryImpl
import com.yeobaek.data.repositoryImpl.remote.UserRepositoryImpl

class AppContainer(
    private val isDebug: Boolean,
) {
    private val settings = Settings()

    val userPreferences = UserPreferences(settings)

    val crashReporter = CrashReporter(isDebug = isDebug)

    val appName = if (isDebug) "조밀" else "여백"

    private val networkProvider = NetworkProvider(
        userPreferences = userPreferences,
        isDebug = isDebug,
    )

    private val apiProvider = ApiProvider(
        ktorfit = networkProvider.ktorfit,
    )

    val userRepository: UserRepository = UserRepositoryImpl(
        userApi = apiProvider.userApi,
        userPreferences = userPreferences,
    )
    val bookRepository: BookRepository = BookRepositoryImpl(
        bookApi = apiProvider.bookApi,
    )
    val commentRepository: CommentRepository = CommentRepositoryImpl(
        commentApi = apiProvider.commentApi,
    )
    val groupRepository: GroupRepository = GroupRepositoryImpl(
        clubApi = apiProvider.clubApi,
    )
    val readerRepository: ReaderRepository = ReaderRepositoryImpl(
        readerApi = apiProvider.readerApi,
    )

    fun close() {
        networkProvider.close()
    }
}
