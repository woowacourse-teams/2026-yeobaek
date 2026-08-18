package com.yeobaek.core.network

import com.yeobaek.data.api.BookApi
import com.yeobaek.data.api.ClubApi
import com.yeobaek.data.api.CommentApi
import com.yeobaek.data.api.ReaderApi
import com.yeobaek.data.api.UserApi
import com.yeobaek.data.api.createBookApi
import com.yeobaek.data.api.createClubApi
import com.yeobaek.data.api.createCommentApi
import com.yeobaek.data.api.createReaderApi
import com.yeobaek.data.api.createUserApi
import de.jensklingenberg.ktorfit.Ktorfit

class ApiProvider(
    ktorfit: Ktorfit,
) {
    val userApi: UserApi = ktorfit.createUserApi()
    val bookApi: BookApi = ktorfit.createBookApi()
    val clubApi: ClubApi = ktorfit.createClubApi()
    val commentApi: CommentApi = ktorfit.createCommentApi()
    val readerApi: ReaderApi = ktorfit.createReaderApi()
}
