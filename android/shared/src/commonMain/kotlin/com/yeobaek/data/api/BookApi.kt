package com.yeobaek.data.api

import com.yeobaek.data.dto.BookDetailResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

interface BookApi {

    @GET("api/books/{bookId}")
    suspend fun getBookDetail(
        @Path("bookId") bookId: Int,
    ): BookDetailResponse
}
