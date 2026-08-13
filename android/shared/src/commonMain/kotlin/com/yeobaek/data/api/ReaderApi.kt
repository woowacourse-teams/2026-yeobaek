package com.yeobaek.data.api

import com.yeobaek.data.dto.PassagesResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

interface ReaderApi {
    @GET("api/clubs/{clubId}/passages")
    suspend fun getPassages(
        @Path("clubId") clubId: Int,
        @Query("from") from: Int,
        @Query("to") to: Int,
    ): PassagesResponse
}
