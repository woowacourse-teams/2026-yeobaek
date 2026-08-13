package com.yeobaek.core.app

import com.yeobaek.data.api.BookApi
import com.yeobaek.data.api.ClubApi
import com.yeobaek.data.api.createBookApi
import com.yeobaek.data.api.createClubApi
import de.jensklingenberg.ktorfit.Ktorfit

class ApiProvider(
    ktrofit: Ktorfit
) {
    val clubApi: ClubApi = ktrofit.createClubApi()

    val booksApi: BookApi = ktrofit.createBookApi()
}
