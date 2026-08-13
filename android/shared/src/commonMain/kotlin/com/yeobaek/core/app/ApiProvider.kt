package com.yeobaek.core.app

import com.yeobaek.data.api.BookApi
import com.yeobaek.data.api.ClubApi
import com.yeobaek.data.api.UserApi
import com.yeobaek.data.api.createBookApi
import com.yeobaek.data.api.createClubApi
import com.yeobaek.data.api.createUserApi
import de.jensklingenberg.ktorfit.Ktorfit

class ApiProvider(
    ktrofit: Ktorfit,
) {
    val clubApi: ClubApi = ktrofit.createClubApi()

    val booksApi: BookApi = ktrofit.createBookApi()

    val userApi: UserApi = ktrofit.createUserApi()
}
