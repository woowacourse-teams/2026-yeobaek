package com.yeobaek.data.api

import com.yeobaek.data.dto.BookDetailResponse
import com.yeobaek.data.dto.BooksResponse
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path

interface BookApi {
    @GET("api/books")
    suspend fun getBooks(): Response<BooksResponse>

    @GET("api/books/{bookId}")
    suspend fun getBookDetail(
        @Path("bookId") bookId: Int,
    ): BookDetailResponse
}
