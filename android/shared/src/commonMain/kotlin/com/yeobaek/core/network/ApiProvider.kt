package com.yeobaek.core.network

import com.yeobaek.data.api.BookApi
import com.yeobaek.data.api.ClubApi
import com.yeobaek.data.api.ReaderApi
import com.yeobaek.data.api.createBookApi
import com.yeobaek.data.api.createClubApi
import com.yeobaek.data.api.createReaderApi
import de.jensklingenberg.ktorfit.Ktorfit

class ApiProvider(
    ktorfit: Ktorfit,
) {
    val bookApi: BookApi = ktorfit.createBookApi()
    val clubApi: ClubApi = ktorfit.createClubApi()
    val readerApi: ReaderApi = ktorfit.createReaderApi()
}
