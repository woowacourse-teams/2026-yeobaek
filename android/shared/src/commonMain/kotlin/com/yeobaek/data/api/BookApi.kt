package com.yeobaek.data.api

import com.yeobaek.data.dto.BookDetailResponse
import com.yeobaek.data.dto.BooksResponse
import com.yeobaek.data.dto.SearchBooksResponse
import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

interface BookApi {
    @GET("api/books")
    suspend fun getBooks(): Response<BooksResponse>

    @GET("api/books/{bookId}")
    suspend fun getBookDetail(
        @Path("bookId") bookId: Long,
    ): BookDetailResponse

    @GET("api/books/")
    suspend fun getSearchBooks(
        @Query keyword: String,
    ): Response<SearchBooksResponse>
}
